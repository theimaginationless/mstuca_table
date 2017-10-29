package com.app.dmitryteplyakov.shedule;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v14.preference.SwitchPreference;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.DividerItemDecoration;
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
import android.widget.TextView;
import com.app.dmitryteplyakov.shedule.Core.Discipline;
import com.app.dmitryteplyakov.shedule.Core.DisciplineStorage;
import com.app.dmitryteplyakov.shedule.Core.TableParser;

import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.util.CellRangeAddress;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
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
import java.util.UUID;

/**
 * Created by Dmitry on 20.07.2017.
 */

public class SheduleListFragment extends Fragment {

    private Discipline mDiscipline;
    private RecyclerView mRecyclerView;
    private SheduleAdapter mAdapter;

    private LinearLayoutManager linearLayoutManager;
    private RecyclerView.SmoothScroller smoothScroller;
    boolean dSuccess;
    private String EXTRA_SAVED;
    private SwipeRefreshLayout mSwipeRefreshData;
    private boolean swipeRefresh;
    private boolean forcedUpdate;
    private boolean notFirstRun;
    private String filename = "shedule.xls";
    private LinearLayoutManager gridLayoutManager;
    private boolean onceDiscipline;
    private boolean isSubgroup;
    private boolean isDbDrop;
    private boolean turnOff;
    private volatile boolean inProcess;
    private static boolean isCourseChanged;
    private static final int REQUEST_DATE = 5;
    private static final String DIALOG_DATE = "com.app.shedulelistfragment.dialog_date";
    private static boolean isNotGlobalChanges;
    private static boolean resetPosition;
    private DividerItemDecoration mDividerItemDecorator;
    private AsyncLoader loader;
    private AsyncUpdater updater;

    public static void setResetPosition(boolean resetPositionArg) {
        resetPosition = resetPositionArg;
    }

    public static void setIsNotGlobalChanges(boolean isNotGlobalChanges) {
        SheduleListFragment.isNotGlobalChanges = isNotGlobalChanges;
    }

    public static void setIsCourseChanged(boolean state) {
        isCourseChanged = state;
    }

    public void dbDropped() {
        isDbDrop = true;
    }

    /*public String pageParser() {
        String links = null;
        try {
            Document doc = Jsoup.connect("http://mstuca.ru/students/schedule/").get();
            Elements metaElements = doc.select("a.section-title");
            links = metaElements.html();
            return links;
        } catch(IOException e) {

        }
        return links;
    }*/


    private boolean downloadFile(Context mContext, int sheet) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        String faculty = sharedPreferences.getString("faculty", "0");
        String spec = sharedPreferences.getString("spec", "0");
        String course = sharedPreferences.getString("course", "0");
        String stream = sharedPreferences.getString("stream", "0");
        String file_url = null;
        String fix = getString(R.string.m);
        //Log.d("PARSERHTML", pageParser());

        Log.d("sheduleDownloader", "SHEET: " + Integer.toString(sheet));
        if(sheet != 0 && isNotGlobalChanges) {
            Log.d("sheduleDownloader", "Is not global changes in shedule. Skip downloading...");
            return true;
        }

        if (course.equals("0")) {
            turnOff = true;
            Snackbar.make(getActivity().findViewById(R.id.snackbar_layout), getString(R.string.select_started_hint_snackbar), Snackbar.LENGTH_LONG).show();
            return false;
        }
        Log.d("ERERRE", spec);
        String urlPart = null;
        try {
            if(!faculty.equals(getString(R.string.mech_link)) && !spec.equals(getString(R.string.rst)) && !spec.equals(getString(R.string.uvdbobp)))
                urlPart = faculty + "/" + spec.substring(0, spec.length() - 1) + "/" + spec + " " + course + "-" + stream + ".xls";
            else if(faculty.equals(getString(R.string.mech_link)))
                urlPart = faculty + "/" + fix + "/" + spec + " " + course + "-" + stream + ".xls";
            else if(spec.equals(getString(R.string.rst)))
                urlPart = faculty + "/" + getString(R.string.rs) + "/" + spec + " " + course + "-" + stream + ".xls";
            else if(spec.equals(getString(R.string.uvdbobp)))
                urlPart = faculty + "/" + getString(R.string.uvd) + "/" + getString(R.string.uvd) + " " + course + "-" + stream + " " + getString(R.string.obp) +".xls";


            file_url = "http://mstuca.ru/students/schedule/" + URLEncoder.encode(urlPart, "UTF-8").replaceAll("\\+", "%20").replaceAll("%2F", "/");
        } catch(UnsupportedEncodingException e) {

        }
        Log.d("SLFDownloader", "Check " + file_url);
        if(course.equals("0") || faculty.equals("0") || spec.equals("0") || stream.equals("0")) {
            Log.d("SLFDownloader", "Data isn't fully!");
            return false;
        }

        InputStream input = null;
        FileOutputStream output = null;
        FileInputStream localeShedule = null;
        try {
            URL url = new URL(file_url);
            URLConnection connection = url.openConnection();
            Log.d("URL", connection.getURL().toString());
            input = connection.getInputStream();
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            input = connection.getInputStream();
            Log.d("sheduleDownloader", "Type: " + con.getContentType() + " RESP: " + con.getResponseCode() + " RESPMSG: " + con.getResponseMessage());


            String lastModRemoteString = connection.getHeaderField("Last-Modified");
            SimpleDateFormat dateformatter = new SimpleDateFormat("E, d MMM yyyy HH:mm:ss Z", Locale.ENGLISH);
            Date lastModRemote = new Date(connection.getLastModified());
            File localFile = new File(mContext.getFilesDir(), filename);
            Log.d("Already?", Boolean.toString(localFile.exists()));
            Log.d("Course changed?", Boolean.toString(isCourseChanged));


            if (isDbDrop && localFile.exists() && !isCourseChanged)
                return true;

            if (!forcedUpdate && !isCourseChanged) {
                Log.d("SLFDownloader", "Forced updating disabled");
                if (localFile.exists()) {
                    localeShedule = mContext.openFileInput(filename);
                    Date lastModLocal = new Date(localFile.lastModified());
                    Log.d("SLFDownloader", lastModLocal.toString() + " " + lastModRemote.toString());
                    if ((lastModLocal.equals(lastModRemote) || lastModLocal.after(lastModRemote))) {
                        if (swipeRefresh)
                            Snackbar.make(getActivity().findViewById(R.id.snackbar_layout), getString(R.string.data_already_fresh), Snackbar.LENGTH_SHORT).show();
                        Log.d("SLFDownloader", "Data is fresh. Skip downloading...");
                        turnOff = true;
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


            if (swipeRefresh)
                Snackbar.make(getActivity().findViewById(R.id.snackbar_layout), getString(R.string.data_updated), Snackbar.LENGTH_SHORT).show();
            return true;

        } catch (UnknownHostException e) {
            Log.e("SLFDownloader", "Wrong address or cannot connecting to internet?", e);
            turnOff = true;
            return false;
        } catch(FileNotFoundException e) {
            Snackbar.make(getActivity().findViewById(R.id.snackbar_layout), getString(R.string.filenotfound_snackbar), Snackbar.LENGTH_LONG).show();
            turnOff = true;
            return false;
        } catch(IOException e) {
            Log.e("SheduleDownloader", "Error IO " + e);
            turnOff = true;
            return false;
        } finally {
            try {
                if (output != null)
                    output.close();
                if (input != null)
                    input.close();
            } catch (IOException e) {
            }
        }

    }

    @Override
    public void onResume() {
        super.onResume();
        if (DisciplineStorage.get(getActivity()).getDisciplines().size() == 0 && notFirstRun) {
            Log.d("SLF", "Rebase after DB RESET");
            isDbDrop = true;
            AsyncLoader loader = new AsyncLoader(0);
            loader.execute(getActivity());
        }
    }
    /* Old imp
    private void sheduleReader(boolean isNew, int sheet, int labGroup, int langGroup) {
        if (!isNew) {
            Log.d("SHDRDR", "Data is fresh. Skip updating...");
            return;
        }

        String disciplineTitle;
        String disciplineType;
        String teacherName;
        String aud;
        Calendar year = Calendar.getInstance(new Locale("ru"));
        year.setTime(new Date());
        HSSFWorkbook myShedule = null;
        try {
            myShedule = new HSSFWorkbook(((getActivity().openFileInput(filename))));
        } catch (IOException e) {
            Log.e("scheduleReader", "Error read schedule file!");
        }
        try {
            if (myShedule.getNumberOfSheets() < sheet + 1) {
                Log.d("scheduleReader", "Done.");
                return;
            }
        } catch(NullPointerException e) {
            Log.e("scheduleReader", "NPE (monitoring need): " + "sheetNumber: " + Integer.toString(sheet + 1) + " isNew: " + Boolean.toString(isNew) + " Lab/Lang group: " + Integer.toString(labGroup) + "/" + Integer.toString(langGroup), e);
        }
        HSSFSheet mySheduleSheet = myShedule.getSheetAt(sheet);

        List<CellRangeAddress> regions = mySheduleSheet.getMergedRegions();

        for (int rowIndex = 1; rowIndex + 3 <= mySheduleSheet.getLastRowNum(); rowIndex++) {
            if (mySheduleSheet.getRow(rowIndex + 2).getCell(2).getStringCellValue().equals("") || mySheduleSheet.getRow(rowIndex + 1).getCell(2).getStringCellValue().equals("") || mySheduleSheet.getRow(rowIndex).getCell(2).getStringCellValue().equals("")) {
                Log.d("SLF", "EMPTY!");
                continue;
            }

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

            if(disciplineType.equals("Экзамен") || disciplineType.equals("Консультация")) {
                Log.d("scheduleReader", "Skip as workaround: " + disciplineTitle + " " + disciplineType);
                rowIndex += 2;
                onceDiscipline = false;
                continue;
            }

            if (sheet != 0) {
                if (sheet != langGroup && (disciplineType.contains("Пр.Зан.") || disciplineType.contains("Лекция"))) {
                    Log.d("SLF", "skip " + disciplineTitle + " " + teacherName + " subgroup " + Integer.toString(sheet - 1));
                    rowIndex += 2;
                    onceDiscipline = false;
                    Log.d("SLF", "JUMP: OLD: " + Integer.toString(rowIndex - 2) + " NEW: " + Integer.toString(rowIndex - 1));
                    continue;
                }
                if (sheet != labGroup && disciplineType.contains("Лаб.раб.")) {
                    Log.d("SLF", "skip " + disciplineTitle + " " + teacherName + " subgroup " + Integer.toString(sheet - 1));
                    rowIndex += 2;
                    onceDiscipline = false;
                    Log.d("SLF", "JUMP: OLD: " + Integer.toString(rowIndex - 2) + " NEW: " + Integer.toString(rowIndex + 1));
                    continue;
                }
            }

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

            int number = 0;
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
            List<Calendar> onceCalendars = new ArrayList<>();


            if (dateRange.contains("с ")) {
                if (dateRange.contains("кроме")) {
                    date = dateRange.replaceFirst("     с ", "").replace(" по ", "|").replace("       кроме ", "|");
                    Log.d("SLF", "кроме");
                    int sliceIndex = date.indexOf("|");
                    int endSliceIndex = date.indexOf("|", sliceIndex + 1);

                    firstPart = date.substring(0, sliceIndex);
                    secondPart = date.substring(sliceIndex + 1, endSliceIndex);
                    exclusePart = date.substring(endSliceIndex + 1);
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
                firstPart = date;
            }
            dateFormatter = new SimpleDateFormat("dd.MM");
            dateFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
            dateFormatter.applyPattern("dd.MM");

            Calendar startCalendar = null;
            Calendar endCalendar = null;
            Calendar excludeCalendar = null;

            try {
                if (secondPart.equals("") && firstPart.contains(";")) {
                    int firstI = 0;
                    List<String> arrayParts = new ArrayList<>();
                    for (int i = 0; i < firstPart.length(); i++) {
                        if (firstPart.charAt(i) == ';') {
                            arrayParts.add(firstPart.substring(firstI, i));
                            firstI = i + 1;
                        }
                        if (i == firstPart.length() - 1)
                            arrayParts.add(firstPart.substring(firstI, i + 1));
                    }
                    for (String str : arrayParts) {
                        Log.d("SLF", "STR FOR ONCE CALENDARS: " + str);
                        Calendar onceCal = Calendar.getInstance(new Locale("ru"));
                        Date current = dateFormatter.parse(str);
                        onceCal.setTime(current);
                        onceCal.set(Calendar.YEAR, year.get(Calendar.YEAR));
                        onceCal.setFirstDayOfWeek(Calendar.MONDAY);
                        onceCal.set(Calendar.HOUR_OF_DAY, 0);
                        onceCal.set(Calendar.MINUTE, 0);
                        onceCal.set(Calendar.SECOND, 0);
                        onceCal.set(Calendar.MILLISECOND, 0);
                        onceCalendars.add(onceCal);
                    }
                }

                firstDate = dateFormatter.parse(firstPart);
            } catch (ParseException e) {
            }
            startCalendar = Calendar.getInstance(new Locale("ru"));
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
                    endCalendar = Calendar.getInstance(new Locale("ru"));
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

            List<Calendar> excludeCals = new ArrayList<>();
            boolean isExclude = false;
            if (!exclusePart.equals("")) {
                dateFormatter.applyPattern("dd.MM");
                try {
                    int firstI = 0;
                    List<String> arrayParts = new ArrayList<>();
                    for (int i = 0; i < exclusePart.length(); i++) {
                        if (exclusePart.charAt(i) == ';') {
                            arrayParts.add(exclusePart.substring(firstI, i));
                            firstI = i + 1;
                        }
                        if (i == exclusePart.length() - 1)
                            arrayParts.add(exclusePart.substring(firstI, i + 1));
                    }
                    for (String str : arrayParts) {
                        Log.d("SLF", "STR FOR EXCL CALENDARS: " + str);

                        Calendar exclCal = Calendar.getInstance(new Locale("ru"));
                        Date current = dateFormatter.parse(str);
                        exclCal.setTime(current);
                        exclCal.set(Calendar.YEAR, year.get(Calendar.YEAR));
                        exclCal.setFirstDayOfWeek(Calendar.MONDAY);
                        exclCal.set(Calendar.HOUR_OF_DAY, 0);
                        exclCal.set(Calendar.MINUTE, 0);
                        exclCal.set(Calendar.SECOND, 0);
                        exclCal.set(Calendar.MILLISECOND, 0);
                        excludeCals.add(exclCal);
                    }
                    if (arrayParts.size() != 0)
                        isExclude = true;


                    excludeDate = dateFormatter.parse(exclusePart);
                    excludeCalendar = Calendar.getInstance(new Locale("ru"));
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
            if (endCalendar == null)
                endCalendar = startCalendar;
            Calendar cal = Calendar.getInstance(new Locale("ru"));
            cal.setTime(new Date());
            cal.set(Calendar.MONTH, 11);
            cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
            Log.d("TEST", Integer.toString(cal.get(Calendar.DAY_OF_WEEK)));
            int diffCount = 0;
            boolean firstMonth = true;



            int countDates = 1;
            if (onceCalendars.size() != 0)
                countDates = onceCalendars.size();
            for (int i = 0; i < countDates; i++) {
                firstMonth = true;
                int startMonth = startCalendar.get(Calendar.MONTH);
                int endMonth = endCalendar.get(Calendar.MONTH);
                if (onceCalendars.size() != 0) {
                    if (!isExclude) {
                        startCalendar = onceCalendars.get(i);
                        endCalendar = onceCalendars.get(i);
                        startMonth = onceCalendars.get(i).get(Calendar.MONTH);
                        endMonth = onceCalendars.get(i).get(Calendar.MONTH);
                    }
                    Log.d("SLF EXPERIMENTAL", "STARTMONTH: " + Integer.toString(startMonth + 1) + " ENDMONTH: " + Integer.toString(endMonth + 1));
                }
                for (int MONTH = startCalendar.get(Calendar.MONTH); MONTH <= endCalendar.get(Calendar.MONTH); MONTH++) {

                    Log.d("SLF", "MONTH START: " + Integer.toString(MONTH + 1) + " MONTH END: " + Integer.toString(endCalendar.get(Calendar.MONTH) + 1));
                    int startDay;
                    int endDay;
                    Calendar tempStart = Calendar.getInstance(new Locale("ru"));
                    tempStart.set(Calendar.MONTH, MONTH);
                    Calendar tempEnd = Calendar.getInstance(new Locale("ru"));
                    tempEnd.set(Calendar.MONTH, MONTH);
                    Log.d("SLF", "FIRST MONTH: " + Boolean.toString(firstMonth));
                    if (firstMonth) {
                        startDay = startCalendar.get(Calendar.DAY_OF_MONTH);
                    } else {
                        startDay = tempStart.getActualMinimum(Calendar.DAY_OF_MONTH);
                    }
                    endDay = tempStart.getActualMaximum(Calendar.DAY_OF_MONTH);
                    if (startCalendar.get(Calendar.MONTH) == endCalendar.get(Calendar.MONTH)) {
                        startDay = startCalendar.get(Calendar.DAY_OF_MONTH);
                        endDay = endCalendar.get(Calendar.DAY_OF_MONTH);
                        Log.d("SLF", "ONE DAY OR MONTH ONLY! FIRSTDAY: " + Integer.toString(startDay) + " ENDDAY: " + Integer.toString(endDay));
                    } else if (MONTH == endCalendar.get(Calendar.MONTH)) {
                        startDay = tempStart.getActualMinimum(Calendar.DAY_OF_MONTH);
                        endDay = endCalendar.get(Calendar.DAY_OF_MONTH);
                        Log.d("SLF", "LAST MONTH! FIRSTDAY: " + Integer.toString(startDay) + " ENDDAY: " + Integer.toString(endDay));
                    } else {
                        Log.d("SLF", "PERIOD! FIRSTDAY: " + Integer.toString(startDay) + " ENDDAY: " + Integer.toString(endDay));
                    }
                    Log.d("SLF", "DAY START THIS MONTH: " + Integer.toString(startDay) + " DAY END THIS MONTH: " + Integer.toString(endDay));
                    firstMonth = false;
                    for (int DAY = startDay; DAY <= endDay; DAY++) {
                        Calendar resultCalendar = Calendar.getInstance(new Locale("ru"));
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
                        Calendar sept = Calendar.getInstance(new Locale("ru"));
                        sept.set(resultCalendar.get(Calendar.YEAR), Calendar.SEPTEMBER, 1, 0, 0, 0);
                        switch ((resultCalendar.get(Calendar.WEEK_OF_YEAR) - sept.get(Calendar.WEEK_OF_YEAR) + 1) % 2) {
                            case 1:
                                Log.d("WWWW", "MONTH: " + Integer.toString(MONTH + 1) + " DAY: " + Integer.toString(DAY) + " Верхняя");
                                break;
                            case 0:
                                Log.d("WWWW", "MONTH: " + Integer.toString(MONTH + 1) + " DAY: " + Integer.toString(DAY) + " Нижняя");
                                break;
                        }

                        if (isExclude) {
                            //if ((int) onceCalendars.get(i).get(Calendar.DAY_OF_MONTH) == DAY && (int) onceCalendars.get(i).get(Calendar.MONTH) == MONTH) {
                            //for(Calendar calendarExc : excludeCals) {
                            int j = 0;
                            for(j = 0; j < excludeCals.size(); j++) {
                                if ((int) excludeCals.get(j).get(Calendar.DAY_OF_MONTH) == DAY && (int) excludeCals.get(j).get(Calendar.MONTH) == MONTH) {
                                    Log.d("SLF", "EXCLUDE!" + excludeCals.get(j).getTime().toString() + " TITLE: " + disciplineTitle);
                                    break;
                                }
                            }
                            if(j < excludeCals.size())
                                continue;
                            //}
                        }


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

                            Log.d("CAL", Integer.toString(resultCalendar.get(Calendar.WEEK_OF_YEAR) - sept.get(Calendar.WEEK_OF_YEAR) + 1) + " DISCIPLINE CAL: " + Integer.toString(weekInt));
                            if ((resultCalendar.get(Calendar.WEEK_OF_YEAR) - sept.get(Calendar.WEEK_OF_YEAR) + 1) % 2 == weekInt || weekInt == 2) {
                                Log.d("SLF", "TITLE: " + disciplineTitle + " DAY: " + Integer.toString(DAY) + " MONTH: " + Integer.toString(MONTH + 1) + " NUM: " + Integer.toString(number) + " WEEK: " + week + " WEEKCURRENT: " + Integer.toString(resultCalendar.get(Calendar.WEEK_OF_YEAR) - sept.get(Calendar.WEEK_OF_YEAR) + 1) + " CURRENT DAY: " + Integer.toString(DAY));
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
                                if (sheet != 0) {
                                    Log.d("SLF", "SUBGROUP: " + tempDiscipline.getDiscipleName() + " DATE: " + tempDiscipline.getDate().toString());
                                    if (DisciplineStorage.get(getActivity()).getDiscipleByDate(tempDiscipline.getDate()) != null) {
                                        DisciplineStorage.get(getActivity()).deleteDisciplineByDate(tempDiscipline.getDate());
                                        Log.d("SLF", "DELETE OLD");
                                    }
                                }
                                DisciplineStorage.get(getActivity()).addDisciple(tempDiscipline);
                                Log.d("RESULT_ADD", "SHEET: " + Integer.toString(sheet) + " TITLE: " + tempDiscipline.getDiscipleName() + " DATE: " + dateFormatter.format(tempDiscipline.getDate()) + " NUM: " + tempDiscipline.getNumber() + " WEEK: " + week + " WEEKCURRENT: " + Integer.toString(resultCalendar.get(Calendar.WEEK_OF_YEAR) - sept.get(Calendar.WEEK_OF_YEAR) + 1));

                            } else
                                continue;
                        } else
                            continue;
                    }
                }
                Log.d("SLF", "COUNTER DAYS: " + Integer.toString(diffCount));
            }
            rowIndex += 2;
            onceDiscipline = false;
            Log.d("SLF", "JUMP: OLD: " + Integer.toString(rowIndex - 3) + " NEW: " + Integer.toString(rowIndex));
        }
    }
    */


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            if(savedInstanceState.getSerializable(EXTRA_SAVED) != null)
                notFirstRun = (boolean) savedInstanceState.getSerializable(EXTRA_SAVED);
        }
        setRetainInstance(true);


    }

    public boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    private void workingOn(final Context mContext, final int sheet) {
        dSuccess = false;
        Log.d("workingOn", Integer.toString(sheet));
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                if(isNotGlobalChanges) {
                    dSuccess = isNotGlobalChanges;
                    return;
                }
                Log.d("DownloadTask", "Download started!");
                dSuccess = downloadFile(mContext, sheet);
            }
        });
        Thread readThread = new Thread(new Runnable() {
            @Override
            public void run() {
                //sheduleReader(dSuccess, 0, 0, 0);
                //isSubgroup = true;
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
                int langGroup;
                int labGroup;
                if (Integer.parseInt(sharedPreferences.getString("subgroup_lang", getString(R.string.first))) == 1)
                    langGroup = 2;
                else
                    langGroup = 3;
                if (Integer.parseInt(sharedPreferences.getString("subgroup_lab", getString(R.string.first))) == 1)
                    labGroup = 2;
                else
                    labGroup = 3;
                new TableParser(dSuccess, sheet, labGroup, langGroup, filename, mContext);
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

        if (isOnline() || isNotGlobalChanges) {
            workingOn(mContext, sheet);
        } else {
            Snackbar.make(getActivity().findViewById(R.id.snackbar_layout), getString(R.string.error_connection), Snackbar.LENGTH_LONG).show();
            turnOff = true;
        }
        Log.d("SLF", "Connection state: " + Boolean.toString(isOnline()));
    }

    private class AsyncUpdater extends AsyncTask<Context, Void, Void> {
        private Context localContext;

        @Override
        protected void onPreExecute() {
            mSwipeRefreshData.setRefreshing(true);
        }

        @Override
        protected Void doInBackground(Context... contexts) {
            for (Context context : contexts)
                localContext = context;
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            updateUI(localContext);
            mSwipeRefreshData.setRefreshing(false);
            super.onPostExecute(result);
        }
    }

    private class AsyncLoader extends AsyncTask<Context, Void, Void> {
        private Context localContext;
        private int sheet;

        public AsyncLoader(int sheet) {
            this.sheet = sheet;
        }

        @Override
        protected void onPreExecute() {
            mSwipeRefreshData.setRefreshing(true);
            turnOff = false;
            inProcess = true;
            super.onPreExecute();

        }

        @Override
        protected Void doInBackground(Context... contexts) {
            for (Context context : contexts)
                localContext = context;
            //DisciplineStorage.get(localContext).resetDb();
            Log.d("AsyncLoader PRE", "Size: " + Integer.toString(DisciplineStorage.get(localContext).getDisciplines().size()));
            //for(int i = 0; i < 4; i++) {
            //    if(i == 1)
            //        continue;
            //    forcedUpdate = true;
            //    Log.d("Thread CHECKSTARTER", "SHEET INDEX: " + Integer.toString(i));
            //    checkStarter(localContext, i);
            if(DisciplineStorage.get(localContext).getDisciplines().size() == 0)
                forcedUpdate = true;
            Log.d("AsyncLoader", "Forced update: " + Boolean.toString(forcedUpdate));
            checkStarter(localContext, 0);
            if(turnOff)
                return null;
            setIsNotGlobalChanges(true);
            checkStarter(localContext, 2);
            checkStarter(localContext, 3);
            //}
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            Log.d("AsyncLoader", "Thread closed.");
            updateUI(localContext);
            mSwipeRefreshData.setRefreshing(false);
            if (isDbDrop)
                isDbDrop = false;
            setIsCourseChanged(false);
            if(resetPosition) {
                Log.d("AsyncLoader", "Thread closed.");
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        int pos = DisciplineStorage.get(getActivity()).countDisciplinesToDate(new Date(), null);
                        smoothScroller.setTargetPosition(pos);
                        //linearLayoutManager.startSmoothScroll(smoothScroller);
                        //gridLayoutManager.startSmoothScroll(smoothScroller);
                        linearLayoutManager.scrollToPositionWithOffset(pos, 0);
                    }
                });
            }
            setResetPosition(false);
            setIsNotGlobalChanges(false);
            inProcess = false;
            Log.d("AsyncLoader AFTER", Integer.toString(DisciplineStorage.get(localContext).getDisciplines().size()));
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
                AsyncLoader loader = new AsyncLoader(1);
                loader.execute(getActivity());

            }
        });
        mDividerItemDecorator = new DividerItemDecoration(mRecyclerView.getContext(), linearLayoutManager.getOrientation());
        mRecyclerView.addItemDecoration(mDividerItemDecorator);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());

        //if(DisciplineStorage.get(getActivity()).getDisciplines().size() == 0) {
        if (!notFirstRun && sharedPreferences.getBoolean("check_update_when_start", true)) {
            Log.d("SLF", "First check updates start");
            AsyncLoader loader = new AsyncLoader(0);
            loader.execute(getActivity());
        } else if (DisciplineStorage.get(getActivity()).getDisciplines().size() == 0) {
            isDbDrop = true;
            AsyncLoader loader = new AsyncLoader(0);
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
                        //linearLayoutManager.startSmoothScroll(smoothScroller);
                        //gridLayoutManager.startSmoothScroll(smoothScroller);
                        linearLayoutManager.scrollToPositionWithOffset(pos, 0);
                    }
                });
            }
        });
        //updateUI(getActivity());

        return v;
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
            mPairNumber.setText(Integer.toString(mDiscipline.getNumber()));
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
            SimpleDateFormat dateTitleFormatter = new SimpleDateFormat("EEEE, dd MMMM yyyy");
            SimpleDateFormat timeFormatter = new SimpleDateFormat("HH:mm");
            String headerTitle = dateTitleFormatter.format(mDiscipline.getDate());
            headerTitle = headerTitle.substring(0, 1).toUpperCase() + headerTitle.substring(1);
            mDate.setText(dateFormatter.format(discipline.getDate()));
            mDurationTime.setText(timeFormatter.format(mDiscipline.getDate()) + " - " + timeFormatter.format(new Date(mDiscipline.getEndTime())));
            mGlobalDate.setText(headerTitle);
            mPairNumber.setText(Integer.toString(mDiscipline.getNumber()));

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
                Calendar curr = Calendar.getInstance(new Locale("ru"));
                curr.setTime(mDisciplines.get(position - 1).getDate());
                curr.set(Calendar.HOUR_OF_DAY, 0);
                curr.set(Calendar.MINUTE, 0);
                curr.set(Calendar.SECOND, 0);
                curr.set(Calendar.MILLISECOND, 0);

                Calendar after = Calendar.getInstance(new Locale("ru"));
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

    private synchronized void updateUI(Context mContext) { // Обновление интерфейса. Связывание данных с адаптером
        //List<Discipline> disciplines = DisciplineStorage.get(getActivity()).getDisciplines();
        Calendar calendar = Calendar.getInstance(new Locale("ru"));
        calendar.setTime(new Date());
        //calendar.set(Calendar.MONTH, 8);
        //calendar.set(Calendar.DAY_OF_MONTH, 2);
        List<Discipline> disciplines = DisciplineStorage.get(mContext).getDisciplines();
        Collections.sort(disciplines, Discipline.dateComparator);
        Log.d("DEBUG", Integer.toString(disciplines.size()));
        if (mRecyclerView.getAdapter() == null || mAdapter == null) {
            mAdapter = new SheduleAdapter(disciplines); // Связывание списка данных с адаптером
            //mAdapter.setHasStableIds(true);
            if(isAdded())
                mRecyclerView.setAdapter(mAdapter); // Назначение адаптера к RecyclerView
        } else {
            mAdapter.setDisciplines(disciplines);
            mAdapter.notifyDataSetChanged();
        }
        if (!notFirstRun) {
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
        switch (item.getItemId()) {
            case R.id.refresh_toolbar:
                swipeRefresh = true;
                //checkStarter(getActivity());
                AsyncLoader loader = new AsyncLoader(0);
                loader.execute(getActivity());
                return true;
            case R.id.settings:
                Intent intent = PrefActivity.newIntent(getActivity(), "general");
                startActivity(intent);
                return true;
            case R.id.calendar_picker:
                if((DisciplineStorage.get(getActivity()).getDisciplines().size() != 0) && !inProcess) {
                    FragmentManager manager = getFragmentManager();
                    UUID id = DisciplineStorage.get(getActivity()).getDiscipleByNumber(1).getId();
                    if (DisciplineStorage.get(getActivity()).getDiscipleByDate(new Date()) != null)
                        id = DisciplineStorage.get(getActivity()).getDiscipleByDate(new Date()).getId();
                    CalendarDialog dialog = CalendarDialog.newInstance(id);
                    Log.d("return", DisciplineStorage.get(getActivity()).getDisciple(id).getDiscipleName());
                    dialog.setTargetFragment(SheduleListFragment.this, REQUEST_DATE);
                    dialog.show(manager, DIALOG_DATE);
                }
                return true;
        }

        return onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, final Intent data) {
        if(resultCode == Activity.RESULT_OK) {
            if(requestCode == REQUEST_DATE) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        int pos = DisciplineStorage.get(getActivity()).countDisciplinesToDate((Date) data.getSerializableExtra(CalendarDialog.RETURN_DATE), null);
                        Log.d("RES", Integer.toString(pos));
                        //smoothScroller.setTargetPosition(pos);
                        //linearLayoutManager.startSmoothScroll(smoothScroller);
                        linearLayoutManager.scrollToPositionWithOffset(pos, 0);

                    }
                });
            }
        }
    }

}
