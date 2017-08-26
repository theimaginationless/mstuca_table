package com.app.dmitryteplyakov.shedule.Core;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.app.dmitryteplyakov.shedule.database.DisciplineBaseHelper;
import com.app.dmitryteplyakov.shedule.database.DisciplineCursorWrapper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static com.app.dmitryteplyakov.shedule.database.DisciplineDbSchema.*;

/**
 * Created by Dmitry on 20.07.2017.
 */

public class DisciplineStorage {
    private static DisciplineStorage sDisciplineStorage;
    private Context mContext;
    private SQLiteDatabase mDatabase;

    public static DisciplineStorage get(Context context) {
        if(sDisciplineStorage == null)    sDisciplineStorage = new DisciplineStorage(context);
        return sDisciplineStorage;
    }

    private static ContentValues getContentValues(Discipline discipline) {
        ContentValues values = new ContentValues();
        values.put(DisciplineTable.Cols.DISCIPLINETITLE, discipline.getDiscipleName());
        values.put(DisciplineTable.Cols.UUID, discipline.getId().toString());
        values.put(DisciplineTable.Cols.TEACHERNAME, discipline.getTeacherName());
        values.put(DisciplineTable.Cols.AUDNUMBER, discipline.getAuditoryNumber());
        values.put(DisciplineTable.Cols.DATE, discipline.getDate().getTime());
        values.put(DisciplineTable.Cols.TYPE, discipline.getType());
        values.put(DisciplineTable.Cols.NUMBER, discipline.getNumber());

        return values;
    }

    private DisciplineCursorWrapper queryDiscipline(String whereClause, String[] whereArgs) {
        Cursor cursor = mDatabase.query(
                DisciplineTable.NAME,
                null,
                whereClause,
                whereArgs,
                null,
                null,
                null
        );
        return new DisciplineCursorWrapper(cursor);
    }

    public List<Discipline> getDisciplines() {
        List<Discipline> disciplines = new ArrayList<>();
        DisciplineCursorWrapper cursor = queryDiscipline(null, null);
        try {
            cursor.moveToFirst();
            while(!cursor.isAfterLast()) {
                disciplines.add(cursor.getDiscipline());
                cursor.moveToNext();
            }
        } finally {
            cursor.close();
        }
        return disciplines;
    }

    public List<Discipline> getDisciplinesByDate(Date date) {
        List<Discipline> disciplines = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        long startBound = calendar.getTime().getTime();
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);

        long endBound = calendar.getTime().getTime();

        DisciplineCursorWrapper cursor = queryDiscipline("CAST(" + DisciplineTable.Cols.DATE + " AS TEXT) BETWEEN " + "CAST(? AS TEXT) AND " + "CAST(? AS TEXT)",
                new String[] {Long.toString(startBound), Long.toString(endBound)}
        );
        /*DisciplineCursorWrapper cursor = queryDiscipline("CAST(" + DisciplineTable.Cols.DATE + " AS TEXT) = ?",
                new String[]{Long.toString(startBound), Long.toString(endBound)}
        );*/
        try {
            cursor.moveToFirst();
            while(!cursor.isAfterLast()) {
                disciplines.add(cursor.getDiscipline());
                cursor.moveToNext();
            }
        } finally {
            cursor.close();
        }
        Collections.sort(disciplines);
        return disciplines;
    }

    private DisciplineStorage(Context context) {
        mContext = context.getApplicationContext();
        mDatabase = new DisciplineBaseHelper(mContext).getWritableDatabase();
    }

    public Discipline getDisciple(UUID id) {
        DisciplineCursorWrapper cursor = queryDiscipline(DisciplineTable.Cols.UUID + " = ?",
                new String[]{id.toString()}
        );
        try {
            cursor.moveToFirst();
            if(cursor.getCount() == 0) return null;
            return cursor.getDiscipline();
        } finally {
            cursor.close();
        }
    }

    public void addDisciple(Discipline discipline) {
        ContentValues values = getContentValues(discipline);
        mDatabase.insert(DisciplineTable.NAME, null, values);
    }

    public void updateDiscipline(Discipline discipline) {
        ContentValues values = getContentValues(discipline);
        mDatabase.update(DisciplineTable.NAME, values, DisciplineTable.Cols.UUID + " = ?",
                new String[]{discipline.getId().toString()}
        );
    }

    public void deleteDiscipline(Discipline discipline) {
        mDatabase.delete(DisciplineTable.NAME, DisciplineTable.Cols.UUID + " = ?",
                new String[]{discipline.getId().toString()}
        );
    }
}
