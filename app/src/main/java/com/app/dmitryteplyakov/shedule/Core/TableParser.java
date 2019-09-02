package com.app.dmitryteplyakov.shedule.Core;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.util.CellRangeAddress;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Created by dmitry21 on 29.10.17.
 */

public class TableParser {
    private boolean onceDiscipline;
    private static final String TAG = "TableParser";
    private Context mContext;
    private int mSheet;
    private int mLabGroup;
    private int mLangGroup;
    private String mFileName;
    private boolean mIsNew;
    private SharedPreferences pref;

    public TableParser(boolean isNew, int sheet, int labGroup, int langGroup, String filename, Context context) {
        mIsNew = isNew;
        mSheet = sheet;
        mLabGroup = labGroup;
        mLangGroup = langGroup;
        mFileName = filename;
        mContext = context.getApplicationContext();
        pref = PreferenceManager.getDefaultSharedPreferences(mContext);
    }
    public void parse() {

        if (!mIsNew) {
            Log.d(TAG, "Data is fresh. Skip updating...");
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
            myShedule = new HSSFWorkbook(((mContext.openFileInput(mFileName))));
        } catch (IOException e) {
            Log.e(TAG, "Error read schedule file! File: " + mFileName);
        }
        try {
            if (myShedule.getNumberOfSheets() < mSheet + 1) {
                Log.d(TAG, "Done.");
                return;
            }
        } catch(NullPointerException e) {
            Log.e(TAG, "NPE (monitoring need): " + "sheetNumber: " + Integer.toString(mSheet + 1) + " mIsNew: " + Boolean.toString(mIsNew) + " Lab/Lang group: " + Integer.toString(mLabGroup) + "/" + Integer.toString(mLangGroup), e);
        }
        if(myShedule == null) {
            Log.e(TAG, "Sheet not found: fname: " + mFileName + "; Sheet num: " + mSheet);
            return;
        }
        HSSFSheet mySheduleSheet = myShedule.getSheetAt(mSheet);

        List<CellRangeAddress> regions = mySheduleSheet.getMergedRegions();

        for (int rowIndex = 1; rowIndex + 3 <= mySheduleSheet.getLastRowNum(); rowIndex++) {
            if (mySheduleSheet.getRow(rowIndex + 2).getCell(2).getStringCellValue().equals("") || mySheduleSheet.getRow(rowIndex + 1).getCell(2).getStringCellValue().equals("") || mySheduleSheet.getRow(rowIndex).getCell(2).getStringCellValue().equals("")) {
                Log.d(TAG, "EMPTY!");
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
            /**
             * Ohh, it's a workaround...
             */
            if(disciplineType.equals("Экзамен") || disciplineType.equals("Консультация") || disciplineType.equals("Зачет(диф.зач.)") || (disciplineTitle.equals("Иностранный язык") && (mSheet == 0))) {
                Log.d(TAG, "Skip as workaround: " + disciplineTitle + " " + disciplineType);
                rowIndex += 2;
                onceDiscipline = false;
                continue;
            }

            if (mSheet != 0) {
                if(disciplineTitle.contains("Иностранный")) {
                    disciplineType = "Пр.Зан.";
                }
                if (mSheet != mLangGroup && (disciplineType.contains("Пр.Зан.") || disciplineType.contains("Лекция"))) {
                    Log.d(TAG, "skip " + disciplineTitle + " " + teacherName + " subgroup " + Integer.toString(mSheet - 1));
                    rowIndex += 2;
                    onceDiscipline = false;
                    Log.d(TAG, "JUMP: OLD: " + Integer.toString(rowIndex - 2) + " NEW: " + Integer.toString(rowIndex - 1));
                    continue;
                }
                if (mSheet != mLabGroup && disciplineType.contains("Лаб.раб.")) {
                    Log.d(TAG, "skip " + disciplineTitle + " " + teacherName + " subgroup " + Integer.toString(mSheet - 1));
                    rowIndex += 2;
                    onceDiscipline = false;
                    Log.d(TAG, "JUMP: OLD: " + Integer.toString(rowIndex - 2) + " NEW: " + Integer.toString(rowIndex + 1));
                    continue;
                }
            }

            // Weeks
            for (CellRangeAddress region : regions) {
                if (region.isInRange(rowIndex, 1)) {
                    for (int i = 0; i <= region.getLastRow(); i++) {
                        Log.d(TAG, "PRE: " + mySheduleSheet.getRow(region.getFirstRow()).getCell(1).getStringCellValue());
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
                        Log.d(TAG, "INDEX: " + Integer.toString(rowIndex) + " PRE: " + (int) mySheduleSheet.getRow(region.getFirstRow()).getCell(0).getNumericCellValue());
                        if ((int) mySheduleSheet.getRow(region.getFirstRow()).getCell(0).getNumericCellValue() == 0)
                            continue;
                        number = (int) mySheduleSheet.getRow(region.getFirstRow()).getCell(0).getNumericCellValue();
                        break;
                    }
                }
            }

            Log.d(TAG, "TITLE: " + disciplineTitle + " WEEK: " + week);


            SimpleDateFormat dateFormatter = null;
            Date firstDate = null;
            Date secondDate = null;
            Date excludeDate = null;
            List<Calendar> onceCalendars = new ArrayList<>();


            if (dateRange.contains("с ")) {
                if (dateRange.contains("кроме")) {
                    date = dateRange.replaceFirst("     с ", "").replace(" по ", "|").replace("       кроме ", "|");
                    Log.d(TAG, "кроме");
                    int sliceIndex = date.indexOf("|");
                    int endSliceIndex = date.indexOf("|", sliceIndex + 1);

                    firstPart = date.substring(0, sliceIndex);
                    secondPart = date.substring(sliceIndex + 1, endSliceIndex);
                    exclusePart = date.substring(endSliceIndex + 1);
                } else {
                    Log.d(TAG, "С!!!");
                    date = dateRange.replaceFirst("     с ", "").replace(" по ", "|");
                    int sliceIndex = date.indexOf("|");
                    firstPart = date.substring(0, sliceIndex);
                    secondPart = date.substring(sliceIndex + 1);
                }
            } else if (dateRange.contains("только ")) {
                Log.d(TAG, "Только!");
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
                        Log.d(TAG, "STR FOR ONCE CALENDARS: " + str);
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

            Log.d(TAG, "FIRST: " + startCalendar.getTime().toString());

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
                        Log.d(TAG, "STR FOR EXCL CALENDARS: " + str);

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
            Log.d(TAG, Integer.toString(cal.get(Calendar.DAY_OF_WEEK)));
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
                    Log.d(TAG, "STARTMONTH: " + Integer.toString(startMonth + 1) + " ENDMONTH: " + Integer.toString(endMonth + 1));
                }
                for (int MONTH = startCalendar.get(Calendar.MONTH); MONTH <= endCalendar.get(Calendar.MONTH); MONTH++) {

                    Log.d(TAG, "MONTH START: " + Integer.toString(MONTH + 1) + " MONTH END: " + Integer.toString(endCalendar.get(Calendar.MONTH) + 1));
                    int startDay;
                    int endDay;
                    Calendar tempStart = Calendar.getInstance(new Locale("ru"));
                    tempStart.set(Calendar.MONTH, MONTH);
                    Calendar tempEnd = Calendar.getInstance(new Locale("ru"));
                    tempEnd.set(Calendar.MONTH, MONTH);
                    Log.d(TAG, "FIRST MONTH: " + Boolean.toString(firstMonth));
                    if (firstMonth) {
                        startDay = startCalendar.get(Calendar.DAY_OF_MONTH);
                    } else {
                        startDay = tempStart.getActualMinimum(Calendar.DAY_OF_MONTH);
                    }
                    endDay = tempStart.getActualMaximum(Calendar.DAY_OF_MONTH);
                    if (startCalendar.get(Calendar.MONTH) == endCalendar.get(Calendar.MONTH)) {
                        startDay = startCalendar.get(Calendar.DAY_OF_MONTH);
                        endDay = endCalendar.get(Calendar.DAY_OF_MONTH);
                        Log.d(TAG, "ONE DAY OR MONTH ONLY! FIRSTDAY: " + Integer.toString(startDay) + " ENDDAY: " + Integer.toString(endDay));
                    } else if (MONTH == endCalendar.get(Calendar.MONTH)) {
                        startDay = tempStart.getActualMinimum(Calendar.DAY_OF_MONTH);
                        endDay = endCalendar.get(Calendar.DAY_OF_MONTH);
                        Log.d(TAG, "LAST MONTH! FIRSTDAY: " + Integer.toString(startDay) + " ENDDAY: " + Integer.toString(endDay));
                    } else {
                        Log.d(TAG, "PERIOD! FIRSTDAY: " + Integer.toString(startDay) + " ENDDAY: " + Integer.toString(endDay));
                    }
                    Log.d(TAG, "DAY START THIS MONTH: " + Integer.toString(startDay) + " DAY END THIS MONTH: " + Integer.toString(endDay));
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
                        Boolean isEvenInverse = pref.getBoolean("even_inverse", false);
                        int even = (resultCalendar.get(Calendar.WEEK_OF_YEAR) - sept.get(Calendar.WEEK_OF_YEAR)) % 2;
                        if(isEvenInverse) {
                            even = (resultCalendar.get(Calendar.WEEK_OF_YEAR) - sept.get(Calendar.WEEK_OF_YEAR) + 1) % 2;
                        }
                        Log.d("Parser", "even is " + even);
                        switch (even) {
                            case 1:
                                Log.d(TAG, "MONTH: " + Integer.toString(MONTH + 1) + " DAY: " + Integer.toString(DAY) + " Верхняя");
                                break;
                            case 0:
                                Log.d(TAG, "MONTH: " + Integer.toString(MONTH + 1) + " DAY: " + Integer.toString(DAY) + " Нижняя");
                                break;
                        }

                        if (isExclude) {
                            //if ((int) onceCalendars.get(i).get(Calendar.DAY_OF_MONTH) == DAY && (int) onceCalendars.get(i).get(Calendar.MONTH) == MONTH) {
                            //for(Calendar calendarExc : excludeCals) {
                            int j = 0;
                            for(j = 0; j < excludeCals.size(); j++) {
                                if ((int) excludeCals.get(j).get(Calendar.DAY_OF_MONTH) == DAY && (int) excludeCals.get(j).get(Calendar.MONTH) == MONTH) {
                                    Log.d(TAG, "EXCLUDE!" + excludeCals.get(j).getTime().toString() + " TITLE: " + disciplineTitle);
                                    break;
                                }
                            }
                            if(j < excludeCals.size())
                                continue;
                            //}
                        }


                        int weekInt = 2;
                        Log.d("Parser weeks", week);
                        if (week.equals("В"))
                            weekInt = 1;
                        else if (week.equals("Н"))
                            weekInt = 0;
                        int startWeek = startCalendar.get(Calendar.DAY_OF_WEEK);
                        int endWeek = resultCalendar.get(Calendar.DAY_OF_WEEK);
                        //Log.d(TAG, "Week compared: Start: " + Integer.toString(startWeek) + " Result: " + Integer.toString(endWeek));

                        if (startWeek == endWeek) {
                            //Log.d(TAG, "COMPARED!");

                            Log.d(TAG, Integer.toString(Math.abs(resultCalendar.get(Calendar.WEEK_OF_YEAR) - sept.get(Calendar.WEEK_OF_YEAR) + 1)) + " DISCIPLINE CAL: " + Integer.toString(weekInt));
                            int weekEven = (Math.abs(resultCalendar.get(Calendar.WEEK_OF_YEAR) - sept.get(Calendar.WEEK_OF_YEAR))) % 2;
                            if(isEvenInverse) {
                                weekEven = (Math.abs(resultCalendar.get(Calendar.WEEK_OF_YEAR) - sept.get(Calendar.WEEK_OF_YEAR) + 1));
                            }
                            if (weekEven == weekInt || weekInt == 2) {
                                Log.d(TAG, "TITLE: " + disciplineTitle + " DAY: " + Integer.toString(DAY) + " MONTH: " + Integer.toString(MONTH + 1) + " NUM: " + Integer.toString(number) + " WEEK: " + week + " WEEKCURRENT: " + Integer.toString(resultCalendar.get(Calendar.WEEK_OF_YEAR) - sept.get(Calendar.WEEK_OF_YEAR) + 1) + " CURRENT DAY: " + Integer.toString(DAY));
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
                                        Log.e(TAG, "Error getting number! IndexRow: " + rowIndex, new Exception());
                                        break;
                                }

                                tempDiscipline.setDate(resultCalendar.getTime());
                                Log.d(TAG, resultCalendar.getTime().toString());
                                tempDiscipline.setAuditoryNumber(aud);
                                tempDiscipline.setDiscipleName(disciplineTitle);
                                tempDiscipline.setTeacherName(teacherName);
                                /**
                                 * Deprecated code (replace by first-time break.
                                 */
                                /*if (mSheet != 0) {
                                    Log.d(TAG, "SUBGROUP: " + tempDiscipline.getDiscipleName() + " DATE: " + tempDiscipline.getDate().toString());
                                    if (DisciplineStorage.get(mContext).getDiscipleByDate(tempDiscipline.getDate()) != null) {
                                        DisciplineStorage.get(mContext).deleteDisciplineByDate(tempDiscipline.getDate());
                                        Log.d(TAG, "DELETE OLD");
                                    }
                                }*/
                                DisciplineStorage.get(mContext).addDisciple(tempDiscipline);
                                Log.d(TAG, "SHEET: " + Integer.toString(mSheet) + " TITLE: " + tempDiscipline.getDiscipleName() + " DATE: " + dateFormatter.format(tempDiscipline.getDate()) + " NUM: " + tempDiscipline.getNumber() + " WEEK: " + week + " WEEKCURRENT: " + Integer.toString(resultCalendar.get(Calendar.WEEK_OF_YEAR) - sept.get(Calendar.WEEK_OF_YEAR) + 1));

                            } else
                                continue;
                        } else
                            continue;
                    }
                }
                Log.d(TAG, "COUNTER DAYS: " + Integer.toString(diffCount));
            }
            rowIndex += 2;
            onceDiscipline = false;
            Log.d(TAG, "JUMP: OLD: " + Integer.toString(rowIndex - 3) + " NEW: " + Integer.toString(rowIndex));
        }
    }
}
