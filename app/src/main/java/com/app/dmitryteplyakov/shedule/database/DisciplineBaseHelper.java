package com.app.dmitryteplyakov.shedule.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.app.dmitryteplyakov.shedule.Core.DisciplineStorage;

import static com.app.dmitryteplyakov.shedule.database.DisciplineDbSchema.*;

/**
 * Created by dmitry21 on 25.08.17.
 */

public class DisciplineBaseHelper extends SQLiteOpenHelper {
    private static final int VERSION = 1;
    private static final String DATABASE_NAME = "disciplineBase.db";

    public DisciplineBaseHelper(Context context) {
        super(context, DATABASE_NAME, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table " + DisciplineTable.NAME + "(" +
        " _id integer primary key autoincrement, " +
        DisciplineTable.Cols.DISCIPLINETITLE + ", " +
        DisciplineTable.Cols.TEACHERNAME + ", " +
        DisciplineTable.Cols.UUID + ", " +
        DisciplineTable.Cols.NUMBER + ", " +
        DisciplineTable.Cols.TYPE + ", " +
        DisciplineTable.Cols.AUDNUMBER + ", " +
        DisciplineTable.Cols.DATE +
        ")"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
