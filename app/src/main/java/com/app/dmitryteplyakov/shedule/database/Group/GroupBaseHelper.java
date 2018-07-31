package com.app.dmitryteplyakov.shedule.database.Group;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import static com.app.dmitryteplyakov.shedule.database.DisciplineDbSchema.DisciplineTable;
import static com.app.dmitryteplyakov.shedule.database.Group.GroupDbSchema.*;

/**
 * Created by dmitry21 on 25.08.17.
 */

public class GroupBaseHelper extends SQLiteOpenHelper {
    private static final int VERSION = 1;
    private static final String DATABASE_NAME = "groupsBase.db";

    public GroupBaseHelper(Context context) {
        super(context, DATABASE_NAME, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table " + DisciplineTable.NAME + "(" +
        " _id integer primary key autoincrement, " +
        GroupTable.Cols.TITLE + ", " +
        GroupTable.Cols.LINK + ", " +
        GroupTable.Cols.UUID + ", " +
        GroupTable.Cols.FACULTY + ", " +
        ")"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
