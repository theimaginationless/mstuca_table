package com.app.dmitryteplyakov.shedule;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.app.dmitryteplyakov.shedule.Core.Discipline;
import com.app.dmitryteplyakov.shedule.Core.DisciplineStorage;

import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.util.CellRangeAddress;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
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
    private ProgressBar mProgressBar;

    private void downloadFile() {
        InputStream input = null;
        FileOutputStream output = null;
        try {
            URL url = new URL(file_url);
            input = url.openConnection().getInputStream();
            output = getActivity().openFileOutput(filename, Context.MODE_PRIVATE);

            int read;
            byte[] data = new byte[1024];
            while((read = input.read(data)) != -1)  output.write(data, 0, read);
            output.flush();
        } catch(IOException e) {
            Log.e("SheduleDownloader", "Error IO " + e);
        } finally {
            try {
                if (output != null)
                    output.close();
                if (input != null)
                    input.close();
            } catch(IOException e) {}
        }
    }

    private void sheduleReader() {
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
        } catch(IOException e) {
            Log.e("sheduleReader", "Error read shedule file!");
        }
        HSSFSheet mySheduleSheet = myShedule.getSheetAt(0);
        mProgressBar.setMax(mySheduleSheet.getLastRowNum());

        List<CellRangeAddress> regions = mySheduleSheet.getMergedRegions();

        for(int rowIndex = 1; rowIndex + 3 <= mySheduleSheet.getLastRowNum(); rowIndex++) {
            if(mySheduleSheet.getRow(rowIndex + 2).getCell(2).getStringCellValue().equals("") || mySheduleSheet.getRow(rowIndex + 1).getCell(2).getStringCellValue().equals("") || mySheduleSheet.getRow(rowIndex).getCell(2).getStringCellValue().equals("")) {
                Log.d("SLF", "EMPTY!");
                continue;
            }



            //HSSFRow row = mySheduleSheet.getRow(1);
            Discipline discipline = new Discipline();

            mProgressBar.setProgress(rowIndex);
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
            for(CellRangeAddress region : regions) {
                if(region.isInRange(rowIndex, 1)) {
                    for(int i = 0; i <= region.getLastRow(); i++) {
                        Log.d("WEEKTEST", "PRE: " + mySheduleSheet.getRow(region.getFirstRow()).getCell(1).getStringCellValue());
                        if(mySheduleSheet.getRow(region.getFirstRow()).getCell(1).getStringCellValue().equals(""))
                            continue;
                        week = mySheduleSheet.getRow(region.getFirstRow()).getCell(1).getStringCellValue();
                        break;
                    }
                }
            }

            //int number = (int) mySheduleSheet.getRow(rowIndex).getCell(0).getNumericCellValue();
            int number = 0;
            // Numbers
            for(CellRangeAddress region : regions) {
                if(region.isInRange(rowIndex, 0)) {
                    for(int i = 0; i <= region.getLastRow(); i++) {
                        Log.d("WEEKTEST", "INDEX: " + Integer.toString(rowIndex) + " PRE: " + (int) mySheduleSheet.getRow(region.getFirstRow()).getCell(0).getNumericCellValue());
                        if((int) mySheduleSheet.getRow(region.getFirstRow()).getCell(0).getNumericCellValue() == 0)
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
            if(startCalendar == null)
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
            if(endCalendar == null)
                endCalendar = startCalendar;
            Calendar cal = Calendar.getInstance();
            cal.setTime(new Date());
            cal.set(Calendar.MONTH, 11);
            cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
            Log.d("TEST", Integer.toString(cal.get(Calendar.DAY_OF_WEEK)));

            for (int MONTH = startCalendar.get(Calendar.MONTH); MONTH <= endCalendar.get(Calendar.MONTH); MONTH++) {
                Log.d("SLF", "MONTH START: " + Integer.toString(MONTH + 1)+ " END: " + Integer.toString(endCalendar.get(Calendar.MONTH) + 1));
                for (int DAY = startCalendar.get(Calendar.DAY_OF_MONTH); DAY <= endCalendar.get(Calendar.DAY_OF_MONTH); DAY++) {
                    Calendar resultCalendar = Calendar.getInstance();
                    //resultCalendar.setTime(new Date());

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
                    switch((resultCalendar.get(Calendar.WEEK_OF_YEAR) - sept.get(Calendar.WEEK_OF_YEAR) - 1) % 2) {
                        case 1:
                            Log.d("WWWW", "MONTH: " + Integer.toString(MONTH + 1) + " DAY: " + Integer.toString(DAY) + " Верхняя");
                            break;
                        case 0:
                            Log.d("WWWW", "MONTH: " + Integer.toString(MONTH + 1) + " DAY: " + Integer.toString(DAY) + " Нижняя");
                            break;
                    }

                    if(excludeCalendar != null)
                        if(excludeCalendar.get(Calendar.DAY_OF_MONTH) == DAY && excludeCalendar.get(Calendar.MONTH) == MONTH) {
                            Log.d("SLF", "EXCLUDE!" + excludeCalendar.getTime().toString());
                            continue;
                        }


                    //resultCalendar.set(resultCalendar.get(Calendar.YEAR), MONTH, DAY - 1, 0, 0, 0);
                    int weekInt = 2;
                    if(week.equals("В"))
                        weekInt = 1;
                    else if(week.equals("Н"))
                        weekInt = 0;
                    int startWeek = startCalendar.get(Calendar.DAY_OF_WEEK);
                    int endWeek = resultCalendar.get(Calendar.DAY_OF_WEEK);
                    Log.d("SLF", "Week compared: Start: " + Integer.toString(startWeek) + " Result: " + Integer.toString(endWeek));

                    if(startWeek == endWeek) {
                        Log.d("SLF", "COMPARED! WORKING!");

                        Log.d("CAL", Integer.toString(resultCalendar.get(Calendar.WEEK_OF_YEAR) - sept.get(Calendar.WEEK_OF_YEAR) + 1));
                        if((resultCalendar.get(Calendar.WEEK_OF_YEAR) - sept.get(Calendar.WEEK_OF_YEAR) + 1) % 2 == weekInt || weekInt == 2) {
                            Log.d("SLF", "TITLE: " + disciplineTitle + " DAY: " + Integer.toString(DAY) + " MONTH: " + Integer.toString(MONTH + 1) + " NUM: " + Integer.toString(number) + " WEEK: " + week + " WEEKCURRENT: " + Integer.toString(resultCalendar.get(Calendar.WEEK_OF_YEAR) - sept.get(Calendar.WEEK_OF_YEAR)));
                            Discipline tempDiscipline = new Discipline();
                            tempDiscipline.setNumber(number);
                            tempDiscipline.setType(disciplineType);

                            switch(number) {
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
            //}
            discipline.setTeacherName(disciplineTitle);
            //mDisciples.addDisciple(discipline);
            //Log.d("SLF", "STR: " + disciplineTitle + " TYPE: " + disciplineType + " TEACHER: " + teacherName + " AUD: " + aud + " DATE: " + firstDate.toString() + " " + secondDate.toString() + " NUM: " + Integer.toString(number));
            rowIndex += 2;
            Log.d("SLF", "JUMP: OLD: " + Integer.toString(rowIndex - 3)+ " NEW: " + Integer.toString(rowIndex));
            //break;
        }
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_shedule_list, container, false);
        mProgressBar = (ProgressBar) v.findViewById(R.id.progressbar);
        //ProgressBar mProgressBarDownload = (ProgressBar) v.findViewById(R.id.progressBarDownload);
        mRecyclerView = (RecyclerView) v.findViewById(R.id.shedule_list_recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d("DownloadTask", "Download started!");
                downloadFile();
            }
        });
        thread.start();
        try {
            thread.join();

        } catch(InterruptedException e) {

        }
        mProgressBar.setIndeterminate(true);
        //mProgressBarDownload.setVisibility(View.GONE);
        Log.d("DownloadTask", "Download finished!");
        Thread readThread = new Thread(new Runnable() {
            @Override
            public void run() {
                sheduleReader();
            }
        });
        readThread.start();

        try {
            readThread.join();
        } catch(InterruptedException e) {

        }
        mProgressBar.setEnabled(false);
        mProgressBar.setVisibility(View.INVISIBLE);
        updateUI();
        mRecyclerView.scrollToPosition();
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
        private TextView mGlobalDate;

        public SheduleHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
            mTeacherNameTextView = (TextView) itemView.findViewById(R.id.teacherName); // Связывание объектов макета list_item с переменными
            mDiscipleNameTextView = (TextView) itemView.findViewById(R.id.disciple_name);
            mAuditoryTextView = (TextView) itemView.findViewById(R.id.auditory);
            mLectureTypeTextView = (TextView) itemView.findViewById(R.id.lecture_type);
            mDurationTime = (TextView) itemView.findViewById(R.id.start_time);
            mDate = (TextView) itemView.findViewById(R.id.date);
        }

        public void bindShedule(Discipline discipline) {
            mDiscipline = discipline;
            mTeacherNameTextView.setText(discipline.getTeacherName());
            mDiscipleNameTextView.setText(Integer.toString(discipline.getNumber()) + " " + discipline.getDiscipleName());
            mAuditoryTextView.setText(discipline.getAuditoryNumber());
            mLectureTypeTextView.setText(discipline.getType());
            SimpleDateFormat dateFormatter = new SimpleDateFormat("dd MMM");
            SimpleDateFormat timeFormatter = new SimpleDateFormat("HH:mm");
            mDate.setText(dateFormatter.format(discipline.getDate()));
            mDurationTime.setText(timeFormatter.format(mDiscipline.getDate()) + " - " + timeFormatter.format(new Date(mDiscipline.getEndTime())));
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
        }

        public void bindShedule(Discipline discipline) {
            mDiscipline = discipline;
            mTeacherNameTextView.setText(discipline.getTeacherName());
            mDiscipleNameTextView.setText(Integer.toString(discipline.getNumber()) + " " + discipline.getDiscipleName());
            mAuditoryTextView.setText(discipline.getAuditoryNumber());
            mLectureTypeTextView.setText(discipline.getType());
            SimpleDateFormat dateFormatter = new SimpleDateFormat("dd MMM");
            SimpleDateFormat dateTitleFormatter = new SimpleDateFormat("dd MMMM yyyy");
            SimpleDateFormat timeFormatter = new SimpleDateFormat("HH:mm");
            mDate.setText(dateFormatter.format(discipline.getDate()));
            mDurationTime.setText(timeFormatter.format(mDiscipline.getDate()) + " - " + timeFormatter.format(new Date(mDiscipline.getEndTime())));
            mGlobalDate.setText(dateTitleFormatter.format(mDiscipline.getDate()));
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

        @Override
        public int getItemViewType(int position) {
            if(position != 0) {
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
                if(!curr.getTime().equals(after.getTime())) {
                    if(curr.getTime().after(after.getTime()))
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
            switch(viewType) {
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
            switch(holder.getItemViewType()) {
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

    private void updateUI() { // Обновление интерфейса. Связывание данных с адаптером
        //List<Discipline> disciplines = DisciplineStorage.get(getActivity()).getDisciplines();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.set(Calendar.MONTH, 8);
        calendar.set(Calendar.DAY_OF_MONTH, 2);
        List<Discipline> disciplines = DisciplineStorage.get(getActivity()).getDisciplines();
        Collections.sort(disciplines, Discipline.dateComparator);
        mAdapter = new SheduleAdapter(disciplines); // Связывание списка данных с адаптером
        mRecyclerView.setAdapter(mAdapter); // Назначение адаптера к RecyclerView
    }

}
