package com.app.dmitryteplyakov.shedule.Core;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.app.dmitryteplyakov.shedule.database.DisciplineBaseHelper;
import com.app.dmitryteplyakov.shedule.database.DisciplineCursorWrapper;
import com.app.dmitryteplyakov.shedule.database.DisciplineDbSchema;

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
    private static final String TAG = "DisciplineStorage";

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

    private DisciplineCursorWrapper queryDiscipline() {
        Cursor cursor = mDatabase.query(
                DisciplineTable.NAME,
                null,
                null,
                null,
                null,
                null,
                DisciplineTable.Cols.DATE + " ASC"
        );
        return new DisciplineCursorWrapper(cursor);
    }

    public Discipline getFirstLection() {
        DisciplineCursorWrapper cursor = queryDiscipline();
        Discipline firstDiscipline = null;
        try {
            cursor.moveToFirst();
            firstDiscipline = cursor.getDiscipline();
        } catch (CursorIndexOutOfBoundsException ex) {
            Log.e(TAG, "CursorIndexOutOfBoundsException. Cursor.getCount: " + cursor.getCount() + " Cursor.Position: " + cursor.getPosition()
            + " cursor.isFirst: " + cursor.isFirst() + " cursorisNull: " + cursor.isNull(cursor.getPosition()) + " cursor.isAfterLast: " + cursor.isAfterLast());
        } finally {
            cursor.close();
        }
        return firstDiscipline;
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

    public int countDisciplinesToDate(Date endD, Date startD) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(endD);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Discipline firstDiscipline = getFirstLection();
        Calendar firstLection = Calendar.getInstance();
        firstLection.setTime(firstDiscipline.getDate());
        long endDate = calendar.getTime().getTime();

        if(startD != null)
            calendar.setTime(startD);
        else {
            //calendar.set(Calendar.MONTH, 8);
            calendar.set(Calendar.MONTH, firstLection.get(Calendar.MONTH));
            Log.d("FIRST LECTION", String.valueOf(firstLection.get(Calendar.MONTH)));

            calendar.set(Calendar.DAY_OF_MONTH, 1);
        }
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        long startDate = calendar.getTime().getTime();

        if(endDate - startDate < 0)
            return 0;

        DisciplineCursorWrapper cursor = queryDiscipline("CAST(" + DisciplineTable.Cols.DATE + " AS TEXT) BETWEEN " + "CAST(? AS TEXT) AND " + "CAST(? AS TEXT)",
                new String[]{Long.toString(startDate), Long.toString(endDate)}
        );

        try {
            cursor.moveToFirst();
            return cursor.getCount();
        } finally {
            cursor.close();
        }
    }

    public Discipline getDisciplineByDate(Date date) {
        DisciplineCursorWrapper cursor = queryDiscipline(DisciplineTable.Cols.DATE + " = ?",
                new String[]{date.toString()}
        );
        try {
            cursor.moveToFirst();
            if(cursor.getCount() == 0) return null;
            return cursor.getDiscipline();
        } finally {
            cursor.close();
        }
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
        //mDatabase = new DisciplineBaseHelper(mContext).getWritableDatabase();
        mDatabase = DisciplineBaseHelper.getInstance(mContext).getWritableDatabase();
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

    public Discipline getDiscipleByNumber(int num) {
        DisciplineCursorWrapper cursor = queryDiscipline("CAST (_id" + " AS TEXT) = ?",
                new String[]{Integer.toString(num)}
        );
        try {
            cursor.moveToFirst();
            if(cursor.getCount() == 0) return null;
            return cursor.getDiscipline();
        } finally {
            cursor.close();
        }
    }

    public Discipline getDiscipleByDate(Date date) {
        DisciplineCursorWrapper cursor = queryDiscipline("CAST (" + DisciplineTable.Cols.DATE + " AS TEXT) = ?",
                new String[]{Long.toString(date.getTime()).toString()}
        );
        try {
            cursor.moveToFirst();
            Log.d("DB", Integer.toString(cursor.getCount()));
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

    public void updateDisciplineByDate(Discipline discipline) {
        ContentValues values = getContentValues(discipline);
        mDatabase.update(DisciplineTable.NAME, values, "CAST (" + DisciplineTable.Cols.DATE + " AS TEXT) = ?",
                new String[]{Long.toString(discipline.getDate().getTime())}
        );
    }

    public void deleteDiscipline(Discipline discipline) {
        mDatabase.delete(DisciplineTable.NAME, DisciplineTable.Cols.UUID + " = ?",
                new String[]{discipline.getId().toString()}
        );
    }

    public void deleteDisciplineByDate(Date date) {
        mDatabase.delete(DisciplineTable.NAME, "CAST (" + DisciplineTable.Cols.DATE + " AS TEXT) = ?",
                new String[]{Long.toString(date.getTime())}
        );
    }

    public void resetDb() {
        Log.d("DB", "RESET DB!");
        mDatabase.delete(DisciplineTable.NAME, null, null);
        mDatabase.execSQL("DELETE FROM SQLITE_SEQUENCE WHERE NAME = '" + DisciplineTable.NAME + "'");

    }
}
