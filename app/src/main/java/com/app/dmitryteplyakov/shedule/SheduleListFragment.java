package com.app.dmitryteplyakov.shedule;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.LinearSmoothScroller;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.app.dmitryteplyakov.shedule.Core.Discipline;
import com.app.dmitryteplyakov.shedule.Core.DisciplineStorage;

import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.util.CellRangeAddress;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Created by Dmitry on 20.07.2017.
 */

public class SheduleListFragment extends Fragment {

    private Discipline mDiscipline;
    private RecyclerView mRecyclerView;
    private SheduleAdapter mAdapter;
    private static String file_url = "http://mstuca.ru/students/schedule/webdav_bizproc_history_get/35345/35345/?force_download=1";
    private static String filename = "shedule.xls";
    private LinearLayoutManager linearLayoutManager;
    private RecyclerView.SmoothScroller smoothScroller;
    boolean dSuccess;
    private String EXTRA_SAVED;
    private SwipeRefreshLayout mSwipeRefreshData;
    private boolean swipeRefresh;
    private boolean forcedUpdate;
    private boolean notFirstRun;
    private boolean onceDiscipline;

    private boolean downloadFile(Context mContext) {
        InputStream input = null;
        FileOutputStream output = null;
        FileInputStream localeShedule = null;
        try {
            URL url = new URL(file_url);
            URLConnection connection = url.openConnection();
            input = connection.getInputStream();

            String lastModRemoteString = connection.getHeaderField("Last-Modified");
            SimpleDateFormat dateformatter = new SimpleDateFormat("E, d MMM yyyy HH:mm:ss Z", Locale.ENGLISH);
            Date lastModRemote = new Date(connection.getLastModified());
            File localFile = new File(mContext.getFilesDir() + "/" + filename);
            Log.d("DW", Boolean.toString(localFile.exists()));
            if(!forcedUpdate) {
                Log.d("SLFDownloader", "Forced updating disabled");
                if (localFile.exists()) {
                    localeShedule = mContext.openFileInput(filename);
                    Date lastModLocal = new Date(localFile.lastModified());
                    Log.d("SLFDownloader", lastModLocal.toString() + " " + lastModRemote.toString());

                    if ((lastModLocal.equals(lastModRemote) || lastModLocal.after(lastModRemote))) {
                        if (swipeRefresh)
                            Snackbar.make(getActivity().findViewById(R.id.snackbar_layout), getString(R.string.data_already_fresh), Snackbar.LENGTH_SHORT).show();
                        Log.d("SLFDownloader", "Data is fresh. Skip downloading...");
                        return false;
                    }
                }
            }
            forcedUpdate = false;


            output = mContext.openFileOutput(filename, Context.MODE_PRIVATE);

            int read;
            byte[] data = new byte[1024];
            while ((read = input.read(data)) != -1) output.write(data, 0, read);
            output.flush();
            if(swipeRefresh)
                Snackbar.make(getActivity().findViewById(R.id.snackbar_layout), getString(R.string.data_updated), Snackbar.LENGTH_SHORT).show();
            return true;

        } catch(UnknownHostException e) {
            Log.e("SLFDownloader", "Wrong address or cannot connecting to internet?", e);
            return false;
        } catch (IOException e) {
            Log.e("SheduleDownloader", "Error IO " + e);
        } finally {
            try {
                if (output != null)
                    output.close();
                if (input != null)
                    input.close();
            } catch (IOException e) {}
        }
        return true;

    }

    private void sheduleReader(boolean isNew, int sheet) {
        if (!isNew) {
            Log.d("SHDRDR", "Data is fresh. Skip updating...");
            return;
        }

        DisciplineStorage mDisciples = DisciplineStorage.get(getActivity());
        String disciplineTitle;
        String disciplineType;
        String teacherName;
        String aud;
        Calendar year = Calendar.getInstance();
        year.setTime(new Date());
        List<String> parsedStrList = null;
        HSSFWorkbook myShedule = null;
        try {
            myShedule = new HSSFWorkbook(((getActivity().openFileInput(filename))));
        } catch (IOException e) {
            Log.e("sheduleReader", "Error read shedule file!");
        }
        HSSFSheet mySheduleSheet = myShedule.getSheetAt(sheet);

        List<CellRangeAddress> regions = mySheduleSheet.getMergedRegions();
        for (int rowIndex = 1; rowIndex + 3 <= mySheduleSheet.getLastRowNum(); rowIndex++) {
                if (mySheduleSheet.getRow(rowIndex + 2).getCell(2).getStringCellValue().equals("") || mySheduleSheet.getRow(rowIndex + 1).getCell(2).getStringCellValue().equals("") || mySheduleSheet.getRow(rowIndex).getCell(2).getStringCellValue().equals("")) {
                    Log.d("SLF", "EMPTY!");
                    continue;
                }


            //HSSFRow row = mySheduleSheet.getRow(1);
            Discipline discipline = new Discipline();

            disciplineTitle = mySheduleSheet.getRow(rowIndex).getCell(2).getStringCellValue();
            disciplineType = mySheduleSheet.getRow(rowIndex + 1).getCell(2).getStringCellValue();
            teacherName = mySheduleSheet.getRow(rowIndex + 1).getCell(4).getStringCellValue();
            aud = mySheduleSheet.getRow(rowIndex + 1).getCell(6).getStringCellValue();
            String dateRange = mySheduleSheet.getRow(rowIndex + 2).getCell(2).getStringCellValue();
            String firstPart = "";
            String secondPart = "";
            String exclusePart = "";
            String week = "";
            String date = "";
            /*for(int i = rowIndex; i < rowIndex + 3; i++) {
                if (mySheduleSheet.getRow(rowIndex).getCell(1).getStringCellValue().equals(""))
                    continue;
                week = mySheduleSheet.getRow(rowIndex).getCell(1).getStringCellValue();
                Log.d("SLF", Integer.toString(rowIndex) + " TITLE: " + disciplineTitle + " WEEK: " + week);
                break;
            }*/
            //for(int i = rowIndex; i < )

            // Weeks
            for (CellRangeAddress region : regions) {
                if (region.isInRange(rowIndex, 1)) {
                    for (int i = 0; i <= region.getLastRow(); i++) {
                        Log.d("WEEKTEST", "PRE: " + mySheduleSheet.getRow(region.getFirstRow()).getCell(1).getStringCellValue());
                        if (mySheduleSheet.getRow(region.getFirstRow()).getCell(1).getStringCellValue().equals(""))
                            continue;
                        week = mySheduleSheet.getRow(region.getFirstRow()).getCell(1).getStringCellValue();
                        break;
                    }
                }
            }

            //int number = (int) mySheduleSheet.getRow(rowIndex).getCell(0).getNumericCellValue();
            int number = 0;
            // Numbers
            for (CellRangeAddress region : regions) {
                if (region.isInRange(rowIndex, 0)) {
                    for (int i = 0; i <= region.getLastRow(); i++) {
                        Log.d("WEEKTEST", "INDEX: " + Integer.toString(rowIndex) + " PRE: " + (int) mySheduleSheet.getRow(region.getFirstRow()).getCell(0).getNumericCellValue());
                        if ((int) mySheduleSheet.getRow(region.getFirstRow()).getCell(0).getNumericCellValue() == 0)
                            continue;
                        number = (int) mySheduleSheet.getRow(region.getFirstRow()).getCell(0).getNumericCellValue();
                        break;
                    }
                }
            }

            Log.d("WEEKTEST", "TITLE: " + disciplineTitle + " WEEK: " + week);


            SimpleDateFormat dateFormatter = null;
            Date firstDate = null;
            Date secondDate = null;
            Date excludeDate = null;
            if (dateRange.contains("с ")) {
                if (dateRange.contains("кроме")) {
                    date = dateRange.replaceFirst("     с ", "").replace(" по ", "|").replace("       кроме ", "|");
                    Log.d("SLF", "кроме");
                    int sliceIndex = date.indexOf("|");
                    int endSliceIndex = date.indexOf("|", sliceIndex + 1);

                    //Log.d("SLF", "START " + Integer.toString(sliceIndex + 1) + " END " + Integer.toString(endSliceIndex + 1) + " " + " SOURCE: " + date);
                    firstPart = date.substring(0, sliceIndex);
                    secondPart = date.substring(sliceIndex + 1, endSliceIndex);
                    exclusePart = date.substring(endSliceIndex + 1);
                    //Log.d("SLF", "FIRST: " + firstPart + " SECOND: " + secondPart + " EXCL DATA : " + Integer.toString(endSliceIndex + 1) + " SOURCE: " + date);
                } else {
                    Log.d("SLF", "С!!!");
                    date = dateRange.replaceFirst("     с ", "").replace(" по ", "|");
                    int sliceIndex = date.indexOf("|");
                    firstPart = date.substring(0, sliceIndex);
                    secondPart = date.substring(sliceIndex + 1);
                }
            } else if (dateRange.contains("только ")) {
                Log.d("SLF", "Только!");
                onceDiscipline = true;
                date = dateRange.replaceFirst("     только", "");
                //int sliceIndex = date.indexOf("|");
                //Log.d("SLF", Integer.toString(sliceIndex));
                //firstPart = date.substring(0, sliceIndex);
                Calendar cal = Calendar.getInstance();
                firstPart = date;
                //secondPart = date.substring(sliceIndex + 1);
            }
            dateFormatter = new SimpleDateFormat("dd.MM");
            dateFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
            dateFormatter.applyPattern("dd.MM");

            Calendar startCalendar = null;
            Calendar endCalendar = null;
            Calendar excludeCalendar = null;

            try {
                firstDate = dateFormatter.parse(firstPart);
            } catch (ParseException e) {
            }
            startCalendar = Calendar.getInstance();
            startCalendar.setTime(firstDate);
            startCalendar.set(Calendar.YEAR, year.get(Calendar.YEAR));
            startCalendar.setFirstDayOfWeek(Calendar.MONDAY);

            startCalendar.set(Calendar.HOUR_OF_DAY, 0);
            startCalendar.set(Calendar.MINUTE, 0);
            startCalendar.set(Calendar.SECOND, 0);
            startCalendar.set(Calendar.MILLISECOND, 0);

            Log.d("SLF", "FIRST: " + startCalendar.getTime().toString());

            if (!secondPart.equals("")) {
                dateFormatter.applyPattern("dd.MM");
                try {
                    secondDate = dateFormatter.parse(secondPart);
                    endCalendar = Calendar.getInstance();
                    endCalendar.setTime(secondDate);
                    endCalendar.set(Calendar.YEAR, year.get(Calendar.YEAR));
                    endCalendar.setFirstDayOfWeek(Calendar.MONDAY);

                    endCalendar.set(Calendar.HOUR_OF_DAY, 0);
                    endCalendar.set(Calendar.MINUTE, 0);
                    endCalendar.set(Calendar.SECOND, 0);
                    endCalendar.set(Calendar.MILLISECOND, 0);

                } catch (ParseException e) {

                }
            }
            if (!exclusePart.equals("")) {
                dateFormatter.applyPattern("dd.MM");
                try {
                    excludeDate = dateFormatter.parse(exclusePart);
                    excludeCalendar = Calendar.getInstance();
                    excludeCalendar.setTime(excludeDate);
                    excludeCalendar.set(Calendar.YEAR, year.get(Calendar.YEAR));
                    excludeCalendar.setFirstDayOfWeek(Calendar.MONDAY);

                    excludeCalendar.set(Calendar.HOUR_OF_DAY, 0);
                    excludeCalendar.set(Calendar.MINUTE, 0);
                    excludeCalendar.set(Calendar.SECOND, 0);
                    excludeCalendar.set(Calendar.MILLISECOND, 0);

                } catch (ParseException e) {

                }
            }
            if (startCalendar == null)
                continue;
            List<Calendar> calendars = new ArrayList<>();
            //Log.d("SLF", "MONTH: " + endCalendar.get(Calendar.MONTH) + 1);
            //Log.d("SLF", "DAY: " + endCalendar.get(Calendar.DAY_OF_MONTH));
            /*calendars.add(startCalendar);
            if (!secondPart.equals(""))
                calendars.add(endCalendar);
            if (!exclusePart.equals(""))
                calendars.add(excludeCalendar);
            //for(Calendar calendar : calendars) {
            */
            if (endCalendar == null)
                endCalendar = startCalendar;
            Calendar cal = Calendar.getInstance();
            cal.setTime(new Date());
            cal.set(Calendar.MONTH, 11);
            cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
            Log.d("TEST", Integer.toString(cal.get(Calendar.DAY_OF_WEEK)));
            int diffCount = 0;
            boolean firstMonth = true;
            for (int MONTH = startCalendar.get(Calendar.MONTH); MONTH <= endCalendar.get(Calendar.MONTH); MONTH++) {
                Log.d("SLF", "MONTH START: " + Integer.toString(MONTH + 1) + " MONTH END: " + Integer.toString(endCalendar.get(Calendar.MONTH) + 1));
                /*int startDay = startCalendar.get(Calendar.DAY_OF_MONTH);
                int endDay = startCalendar.getActualMaximum(Calendar.MONTH);*/
                int startDay;
                int endDay;
                Log.d("SLF", "FIRST MONTH: " + Boolean.toString(firstMonth));
                if(firstMonth) {
                    startDay = startCalendar.get(Calendar.DAY_OF_MONTH);
                } else {
                    startDay = startCalendar.getActualMinimum(Calendar.DAY_OF_MONTH);
                }
                endDay = startCalendar.getActualMaximum(Calendar.DAY_OF_MONTH);
                if(startCalendar.get(Calendar.MONTH) == endCalendar.get(Calendar.MONTH)) {
                    startDay = startCalendar.get(Calendar.DAY_OF_MONTH);
                    endDay = endCalendar.get(Calendar.DAY_OF_MONTH);
                    Log.d("SLF", "ТОЛЬКО ОДИН МЕСЯЦ! FIRSTDAY: " + Integer.toString(startDay) + " ENDDAY: " + Integer.toString(endDay));
                } else if(MONTH == endCalendar.get(Calendar.MONTH)) {
                    startDay = startCalendar.getActualMinimum(Calendar.DAY_OF_MONTH);
                    endDay = endCalendar.get(Calendar.DAY_OF_MONTH);
                    Log.d("SLF", "ПОСЛЕДНИЙ МЕСЯЦ! FIRSTDAY: " + Integer.toString(startDay) + " ENDDAY: " + Integer.toString(endDay));
                } else
                    Log.d("SLF", "ПЕРИОД! FIRSTDAY: " + Integer.toString(startDay) + " ENDDAY: " + Integer.toString(endDay));
                Log.d("SLF", "DAY START THIS MONTH: " + Integer.toString(startDay) + " DAY END THIS MONTH: " + Integer.toString(endDay));
                firstMonth = false;
                for (int DAY = /*startCalendar.get(Calendar.DAY_OF_MONTH)*/ startDay; DAY <= /*endCalendar.get(Calendar.DAY_OF_MONTH)*/ endDay; DAY++) {
                    Calendar resultCalendar = Calendar.getInstance();
                    //resultCalendar.setTime(new Date());
                    diffCount++;
                    resultCalendar.set(Calendar.HOUR_OF_DAY, 0);
                    resultCalendar.set(Calendar.MINUTE, 0);
                    resultCalendar.set(Calendar.SECOND, 0);
                    resultCalendar.set(Calendar.MILLISECOND, 0);

                    resultCalendar.setFirstDayOfWeek(Calendar.MONDAY);
                    resultCalendar.set(Calendar.MONTH, MONTH);
                    resultCalendar.set(Calendar.DAY_OF_MONTH, DAY);
                    resultCalendar.set(Calendar.YEAR, year.get(Calendar.YEAR));
                    Calendar sept = Calendar.getInstance();
                    sept.set(resultCalendar.get(Calendar.YEAR), Calendar.SEPTEMBER, 1, 0, 0, 0);
                    switch ((resultCalendar.get(Calendar.WEEK_OF_YEAR) - sept.get(Calendar.WEEK_OF_YEAR) + 1) % 2) {
                        case 1:
                            Log.d("WWWW", "MONTH: " + Integer.toString(MONTH + 1) + " DAY: " + Integer.toString(DAY) + " Верхняя");
                            break;
                        case 0:
                            Log.d("WWWW", "MONTH: " + Integer.toString(MONTH + 1) + " DAY: " + Integer.toString(DAY) + " Нижняя");
                            break;
                    }

                    if (excludeCalendar != null)
                        if (excludeCalendar.get(Calendar.DAY_OF_MONTH) == DAY && excludeCalendar.get(Calendar.MONTH) == MONTH) {
                            Log.d("SLF", "EXCLUDE!" + excludeCalendar.getTime().toString() + " TITLE: " + disciplineTitle);
                            continue;
                        }


                    //resultCalendar.set(resultCalendar.get(Calendar.YEAR), MONTH, DAY - 1, 0, 0, 0);
                    int weekInt = 2;
                    if (week.equals("В"))
                        weekInt = 1;
                    else if (week.equals("Н"))
                        weekInt = 0;
                    int startWeek = startCalendar.get(Calendar.DAY_OF_WEEK);
                    int endWeek = resultCalendar.get(Calendar.DAY_OF_WEEK);
                    Log.d("SLF", "Week compared: Start: " + Integer.toString(startWeek) + " Result: " + Integer.toString(endWeek));

                    if (startWeek == endWeek) {
                        Log.d("SLF", "COMPARED! WORKING!");

                        Log.d("CAL", Integer.toString(resultCalendar.get(Calendar.WEEK_OF_YEAR) - sept.get(Calendar.WEEK_OF_YEAR) + 1));
                        if ((resultCalendar.get(Calendar.WEEK_OF_YEAR) - sept.get(Calendar.WEEK_OF_YEAR) + 1) % 2 == weekInt || weekInt == 2) {
                            Log.d("SLF", "TITLE: " + disciplineTitle + " DAY: " + Integer.toString(DAY) + " MONTH: " + Integer.toString(MONTH + 1) + " NUM: " + Integer.toString(number) + " WEEK: " + week + " WEEKCURRENT: " + Integer.toString(resultCalendar.get(Calendar.WEEK_OF_YEAR) - sept.get(Calendar.WEEK_OF_YEAR)));
                            Discipline tempDiscipline = new Discipline();
                            tempDiscipline.setNumber(number);
                            tempDiscipline.setType(disciplineType);

                            switch (number) {
                                case 1:
                                    resultCalendar.set(Calendar.HOUR_OF_DAY, 8);
                                    resultCalendar.set(Calendar.MINUTE, 30);
                                    break;
                                case 2:
                                    resultCalendar.set(Calendar.HOUR_OF_DAY, 10);
                                    resultCalendar.set(Calendar.MINUTE, 10);
                                    break;
                                case 3:
                                    resultCalendar.set(Calendar.HOUR_OF_DAY, 12);
                                    resultCalendar.set(Calendar.MINUTE, 20);
                                    break;
                                case 4:
                                    resultCalendar.set(Calendar.HOUR_OF_DAY, 14);
                                    resultCalendar.set(Calendar.MINUTE, 00);
                                    break;
                                case 5:
                                    resultCalendar.set(Calendar.HOUR_OF_DAY, 15);
                                    resultCalendar.set(Calendar.MINUTE, 55);
                                    break;
                                case 6:
                                    resultCalendar.set(Calendar.HOUR_OF_DAY, 17);
                                    resultCalendar.set(Calendar.MINUTE, 35);
                                    break;
                                case 7:
                                    resultCalendar.set(Calendar.HOUR_OF_DAY, 19);
                                    resultCalendar.set(Calendar.MINUTE, 15);
                                    break;
                                default:
                                    Log.e("SLF", "Error getting number! IndexRow: " + rowIndex, new Exception());
                                    break;
                            }

                            tempDiscipline.setDate(resultCalendar.getTime());
                            Log.d("TTT", resultCalendar.getTime().toString());
                            tempDiscipline.setAuditoryNumber(aud);
                            tempDiscipline.setDiscipleName(disciplineTitle);
                            tempDiscipline.setTeacherName(teacherName);
                            Log.d("RESULT_ADD", "TITLE: " + tempDiscipline.getDiscipleName() + " DATE: " + dateFormatter.format(tempDiscipline.getDate()) + " NUM: " + tempDiscipline.getNumber() + " WEEK: " + week + " WEEKCURRENT: " + Integer.toString(resultCalendar.get(Calendar.WEEK_OF_YEAR) - sept.get(Calendar.WEEK_OF_YEAR)));

                            DisciplineStorage.get(getActivity()).addDisciple(tempDiscipline);
                        } else
                            continue;
                    } else
                        continue;
                }
            }
            Log.d("SLF", "COUNTER DAYS: " + Integer.toString(diffCount));
            //}

            //mDisciples.addDisciple(discipline);
            //Log.d("SLF", "STR: " + disciplineTitle + " TYPE: " + disciplineType + " TEACHER: " + teacherName + " AUD: " + aud + " DATE: " + firstDate.toString() + " " + secondDate.toString() + " NUM: " + Integer.toString(number));
            rowIndex += 2;
            onceDiscipline = false;
            Log.d("SLF", "JUMP: OLD: " + Integer.toString(rowIndex - 3) + " NEW: " + Integer.toString(rowIndex));
            //break;
        }
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(savedInstanceState != null) {
            notFirstRun = (boolean) savedInstanceState.getSerializable(EXTRA_SAVED);
        }

    }

    public boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    private void workingOn(final Context mContext, final int sheet) {
        dSuccess = false;

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d("DownloadTask", "Download started!");
                dSuccess = downloadFile(mContext);
            }
        });
        Thread readThread = new Thread(new Runnable() {
            @Override
            public void run() {
                sheduleReader(dSuccess, sheet);
            }
        });

        thread.start();

        try {
            thread.join();
        } catch (InterruptedException e) {

        }
        readThread.start();
        try {
            readThread.join();
        } catch (InterruptedException e) {

        }

        if (dSuccess)
            Log.d("DownloadTask", "Download finished!");
        else
            Log.d("DownloadTask", "Download cancelled!");
        swipeRefresh = false;
    }

    private void checkStarter(Context mContext, int sheet) {
        Thread thread = null;
        if(isOnline()) {
            workingOn(mContext, sheet);
        } else {
            Snackbar.make(getActivity().findViewById(R.id.snackbar_layout), getString(R.string.error_connection), Snackbar.LENGTH_LONG).show();
        }
        Log.d("SLF", "Connection state: " + Boolean.toString(isOnline()));
    }

    private class AsyncUpdater extends AsyncTask<Context, Void, Void> {
        private Context localContext;

        @Override
        protected Void doInBackground(Context ... contexts) {
            for(Context context : contexts)
                localContext = context;
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            updateUI(localContext);
            super.onPostExecute(result);
        }
    }

    private class AsyncLoader extends AsyncTask<Context, Void, Void> {
        private Context localContext;

        @Override
        protected void onPreExecute() {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mSwipeRefreshData.setRefreshing(true);
                }
            });
            super.onPreExecute();

        }

        @Override
        protected Void doInBackground(Context ... contexts) {
            for(Context context : contexts)
                localContext = context;
            //DisciplineStorage.get(localContext).resetDb();
            Log.d("Thread", "Size: " + Integer.toString(DisciplineStorage.get(localContext).getDisciplines().size()));
            //for(int i = 0; i < 4; i++) {
            //    if(i == 1)
            //        continue;
            //    forcedUpdate = true;
            //    Log.d("Thread CHECKSTARTER", "SHEET INDEX: " + Integer.toString(i));
            //    checkStarter(localContext, i);
            checkStarter(localContext, 0);
            //}
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            Log.d("AsyncLoader", "Thread closed.");
            ((AppCompatActivity)localContext).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mSwipeRefreshData.setRefreshing(false);

                }
            });
            updateUI(localContext);
            Log.d("AsyncLoader", Integer.toString(DisciplineStorage.get(localContext).getDisciplines().size()));
            super.onPostExecute(result);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_shedule_list, container, false);
        setHasOptionsMenu(true);
        mRecyclerView = (RecyclerView) v.findViewById(R.id.shedule_list_recycler_view);
        mSwipeRefreshData = (SwipeRefreshLayout) v.findViewById(R.id.swipe_refresh);
        smoothScroller = new LinearSmoothScroller(getActivity()) {
            @Override
            protected int getVerticalSnapPreference() {
                return LinearSmoothScroller.SNAP_TO_START;
            }
        };
        linearLayoutManager = new LinearLayoutManager(getActivity());

        mRecyclerView.setLayoutManager(linearLayoutManager);

        mSwipeRefreshData.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                swipeRefresh = true;
                AsyncLoader loader = new AsyncLoader();
                loader.execute(getActivity());

            }
        });
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());

        //if(DisciplineStorage.get(getActivity()).getDisciplines().size() == 0) {
        if(!notFirstRun && sharedPreferences.getBoolean("check_update_when_start", true)) {
            Log.d("SLF", "First check updates start");
            AsyncLoader loader = new AsyncLoader();
            loader.execute(getActivity());
        } else {
            AsyncUpdater updater = new AsyncUpdater();
            updater.execute(getActivity());
        }


        getActivity().findViewById(R.id.toolbar).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            int pos = DisciplineStorage.get(getActivity()).countDisciplinesToDate(new Date(), null);
                            smoothScroller.setTargetPosition(pos);
                            linearLayoutManager.startSmoothScroll(smoothScroller);
                            //linearLayoutManager.scrollToPositionWithOffset(pos, 0);
                        }
                    });
            }
        });

        return v;
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putSerializable(EXTRA_SAVED, notFirstRun);
    }

    // Реализация адаптера
    private class SheduleHolder extends RecyclerView.ViewHolder implements View.OnClickListener { // Реализация Holder. Также реализует интерфейс OnClickListener ля обработки нажатий на View
        public TextView mTeacherNameTextView; // Объекты макета list_item
        public TextView mDiscipleNameTextView;
        public TextView mAuditoryTextView;
        public TextView mLectureTypeTextView;
        public TextView mDate;
        private TextView mDurationTime;
        private Discipline mDiscipline;
        private TextView mPairNumber;

        public SheduleHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
            mTeacherNameTextView = (TextView) itemView.findViewById(R.id.teacherName); // Связывание объектов макета list_item с переменными
            mDiscipleNameTextView = (TextView) itemView.findViewById(R.id.disciple_name);
            mAuditoryTextView = (TextView) itemView.findViewById(R.id.auditory);
            mLectureTypeTextView = (TextView) itemView.findViewById(R.id.lecture_type);
            mDurationTime = (TextView) itemView.findViewById(R.id.start_time);
            mDate = (TextView) itemView.findViewById(R.id.date);
            mPairNumber = (TextView) itemView.findViewById(R.id.pair_counter);
        }

        public void bindShedule(Discipline discipline) {
            mDiscipline = discipline;
            mTeacherNameTextView.setText(discipline.getTeacherName());
            mDiscipleNameTextView.setText(discipline.getDiscipleName());
            mAuditoryTextView.setText(discipline.getAuditoryNumber());
            mLectureTypeTextView.setText(discipline.getType());
            SimpleDateFormat dateFormatter = new SimpleDateFormat("dd MMM");
            SimpleDateFormat timeFormatter = new SimpleDateFormat("HH:mm");
            mDate.setText(dateFormatter.format(discipline.getDate()));
            mDurationTime.setText(timeFormatter.format(mDiscipline.getDate()) + " - " + timeFormatter.format(new Date(mDiscipline.getEndTime())));
            mPairNumber.setText(Integer.toString(mDiscipline.getNumber()) + " " + getString(R.string.pair_counter_text));
        }

        @Override
        public void onClick(View v) {
            //
        }
    }

    private class SheduleHolderDate extends RecyclerView.ViewHolder implements View.OnClickListener { // Реализация Holder. Также реализует интерфейс OnClickListener ля обработки нажатий на View
        public TextView mTeacherNameTextView; // Объекты макета list_item
        public TextView mDiscipleNameTextView;
        public TextView mAuditoryTextView;
        public TextView mLectureTypeTextView;
        public TextView mDate;
        private TextView mDurationTime;
        private Discipline mDiscipline;
        private TextView mGlobalDate;
        private TextView mPairNumber;

        public SheduleHolderDate(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
            mTeacherNameTextView = (TextView) itemView.findViewById(R.id.teacherName); // Связывание объектов макета list_item с переменными
            mDiscipleNameTextView = (TextView) itemView.findViewById(R.id.disciple_name);
            mAuditoryTextView = (TextView) itemView.findViewById(R.id.auditory);
            mLectureTypeTextView = (TextView) itemView.findViewById(R.id.lecture_type);
            mDurationTime = (TextView) itemView.findViewById(R.id.start_time);
            mDate = (TextView) itemView.findViewById(R.id.date);
            mGlobalDate = (TextView) itemView.findViewById(R.id.global_date);
            mPairNumber = (TextView) itemView.findViewById(R.id.pair_counter);
        }

        public void bindShedule(Discipline discipline) {
            mDiscipline = discipline;
            mTeacherNameTextView.setText(discipline.getTeacherName());
            mDiscipleNameTextView.setText(discipline.getDiscipleName());
            mAuditoryTextView.setText(discipline.getAuditoryNumber());
            mLectureTypeTextView.setText(discipline.getType());
            SimpleDateFormat dateFormatter = new SimpleDateFormat("dd MMM");
            SimpleDateFormat dateTitleFormatter = new SimpleDateFormat("dd MMMM yyyy");
            SimpleDateFormat timeFormatter = new SimpleDateFormat("HH:mm");
            mDate.setText(dateFormatter.format(discipline.getDate()));
            mDurationTime.setText(timeFormatter.format(mDiscipline.getDate()) + " - " + timeFormatter.format(new Date(mDiscipline.getEndTime())));
            mGlobalDate.setText(dateTitleFormatter.format(mDiscipline.getDate()));
            mPairNumber.setText(Integer.toString(mDiscipline.getNumber()) + " " + getString(R.string.pair_counter_text));

        }

        @Override
        public void onClick(View v) {
            //
        }
    }

    private class SheduleAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> { // Реализация адаптера
        private List<Discipline> mDisciplines;

        public SheduleAdapter(List<Discipline> disciplines) {
            mDisciplines = disciplines;
        }

        public void setDisciplines(List<Discipline> disciplines) {
            mDisciplines = disciplines;
        }

        @Override
        public int getItemViewType(int position) {
            if (position != 0) {
                Calendar curr = Calendar.getInstance();
                curr.setTime(mDisciplines.get(position - 1).getDate());
                curr.set(Calendar.HOUR_OF_DAY, 0);
                curr.set(Calendar.MINUTE, 0);
                curr.set(Calendar.SECOND, 0);
                curr.set(Calendar.MILLISECOND, 0);

                Calendar after = Calendar.getInstance();
                after.setTime(mDisciplines.get(position).getDate());
                after.set(Calendar.HOUR_OF_DAY, 0);
                after.set(Calendar.MINUTE, 0);
                after.set(Calendar.SECOND, 0);
                after.set(Calendar.MILLISECOND, 0);
                if (!curr.getTime().equals(after.getTime())) {
                    if (curr.getTime().after(after.getTime()))
                        return 1;
                    else
                        return 2;
                } else
                    return 0;
            }
            return 1;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) { // Создание представления
            LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
            View view = null;
            switch (viewType) {
                case 2:
                    view = layoutInflater.inflate(R.layout.shedule_list_item_date, parent, false);
                    return new SheduleHolderDate(view);
                case 1:
                    view = layoutInflater.inflate(R.layout.shedule_list_item_date, parent, false);
                    return new SheduleHolderDate(view);
                case 0:
                    view = layoutInflater.inflate(R.layout.shedule_list_item, parent, false);
                    return new SheduleHolder(view);
            }
            return null;

        }

        @Override
        public void onBindViewHolder(final RecyclerView.ViewHolder holder, final int position) { // Замена содержимого View элементами следующих дисциплин
            Discipline discipline = mDisciplines.get(position);
            switch (holder.getItemViewType()) {
                case 2:
                    ((SheduleHolderDate) holder).bindShedule(mDisciplines.get(position));
                    break;
                case 1:
                    ((SheduleHolderDate) holder).bindShedule(discipline);
                    break;
                case 0:
                    ((SheduleHolder) holder).bindShedule(discipline);
                    break;
            }
        }

        @Override
        public int getItemCount() {
            return mDisciplines.size();
        }
    }

    private void updateUI(Context mContext) { // Обновление интерфейса. Связывание данных с адаптером
        //List<Discipline> disciplines = DisciplineStorage.get(getActivity()).getDisciplines();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        //calendar.set(Calendar.MONTH, 8);
        //calendar.set(Calendar.DAY_OF_MONTH, 2);
        List<Discipline> disciplines = DisciplineStorage.get(mContext).getDisciplines();
        Collections.sort(disciplines, Discipline.dateComparator);
        Log.d("DEBUG", Integer.toString(disciplines.size()));
        if(mAdapter == null) {
            mAdapter = new SheduleAdapter(disciplines); // Связывание списка данных с адаптером
            //mAdapter.setHasStableIds(true);
            mRecyclerView.setAdapter(mAdapter); // Назначение адаптера к RecyclerView
        } else {
            mAdapter.setDisciplines(disciplines);
            mAdapter.notifyDataSetChanged();
        }
        if(!notFirstRun) {
            notFirstRun = true;
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    int pos = DisciplineStorage.get(getActivity()).countDisciplinesToDate(new Date(), null);
                    //smoothScroller.setTargetPosition(pos);
                    //linearLayoutManager.startSmoothScroll(smoothScroller);
                    linearLayoutManager.scrollToPositionWithOffset(pos, 0);
                }
            });
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.toolbar, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.refresh_toolbar:
                swipeRefresh = true;
                //checkStarter(getActivity());
                AsyncLoader loader = new AsyncLoader();
                loader.execute(getActivity());
                return true;
            case R.id.settings:
                Intent intent = PrefActivity.newIntent(getActivity(), "general");
                startActivity(intent);
                return true;
        }

        return onOptionsItemSelected(item);
    }

}
