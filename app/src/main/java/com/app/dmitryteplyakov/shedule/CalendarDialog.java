package com.app.dmitryteplyakov.shedule;


import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.DatePicker;

import com.app.dmitryteplyakov.shedule.Core.Discipline;
import com.app.dmitryteplyakov.shedule.Core.DisciplineStorage;

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Created by dmitry21 on 29.08.17.
 */

public class CalendarDialog extends DialogFragment {
    private DatePicker mDatePicker;
    private static final String ARG_DATEPICKER_UUID = "com.calendardialog.arg_datepicker_uuid";
    public static final String RETURN_DATE = "com.calendardialog.return_date";

    public static CalendarDialog newInstance(UUID id) {
        Bundle args = new Bundle();
        args.putSerializable(ARG_DATEPICKER_UUID, id);
        CalendarDialog fragment = new CalendarDialog();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View v = LayoutInflater.from(getActivity()).inflate(R.layout.date_picker, null);
        mDatePicker = (DatePicker) v.findViewById(R.id.datepicker);
        Calendar calendar = Calendar.getInstance();
        Discipline discipline = DisciplineStorage.get(getActivity()).getDisciple((UUID) getArguments().getSerializable(ARG_DATEPICKER_UUID));
        calendar.setTime(discipline.getDate());
        List<Discipline> disciplineList = DisciplineStorage.get(getActivity()).getDisciplines();
        Calendar currentCal = Calendar.getInstance();
        Collections.sort(disciplineList, Discipline.dateComparator);
        Log.d("TES", disciplineList.get(DisciplineStorage.get(getActivity()).getDisciplines().size() - 1).getDate().toString());
        mDatePicker.setMaxDate(disciplineList.get(DisciplineStorage.get(getActivity()).getDisciplines().size() - 1).getDate().getTime());
        Calendar minDate = Calendar.getInstance();
        minDate.setTime(disciplineList.get(1).getDate());
        mDatePicker.setMinDate(minDate.getTimeInMillis());

        //mDatePicker.init(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH), null);
        return new AlertDialog.Builder(getActivity())
                .setView(v)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Calendar calendar = Calendar.getInstance();
                        Calendar currentDate = Calendar.getInstance();
                        int year = currentDate.get(Calendar.YEAR);
                        int month = currentDate.get(Calendar.MONTH);
                        int day = currentDate.get(Calendar.DAY_OF_MONTH);
                        month = mDatePicker.getMonth();
                        day = mDatePicker.getDayOfMonth();
                        calendar.set(mDatePicker.getYear(), month, day, 0, 0);
                        Log.d("CALENDAR", calendar.getTime().toString());
                        sendResult(Activity.RESULT_OK, calendar.getTime());
                    }
                }).create();
    }

    private void sendResult(int resultCode, Date time) {
        if(resultCode == Activity.RESULT_OK) {
            Intent date = new Intent();
            date.putExtra(RETURN_DATE, time);
            getTargetFragment().onActivityResult(getTargetRequestCode(), resultCode, date);
        }
    }
}
