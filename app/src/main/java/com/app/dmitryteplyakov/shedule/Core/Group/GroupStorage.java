package com.app.dmitryteplyakov.shedule.Core.Group;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.app.dmitryteplyakov.shedule.database.DisciplineBaseHelper;
import com.app.dmitryteplyakov.shedule.database.DisciplineCursorWrapper;
import com.app.dmitryteplyakov.shedule.database.Group.GroupBaseHelper;
import com.app.dmitryteplyakov.shedule.database.Group.GroupCursorWrapper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static com.app.dmitryteplyakov.shedule.database.DisciplineDbSchema.DisciplineTable;
import static com.app.dmitryteplyakov.shedule.database.Group.GroupDbSchema.*;

/**
 * Created by Dmitry on 20.07.2017.
 */

public class GroupStorage {
    private static GroupStorage sDisciplineStorage;
    private Context mContext;
    private SQLiteDatabase mDatabase;

    public static GroupStorage get(Context context) {
        if(sDisciplineStorage == null)    sDisciplineStorage = new GroupStorage(context);
        return sDisciplineStorage;
    }

    private static ContentValues getContentValues(Group group) {
        ContentValues values = new ContentValues();
        values.put(GroupTable.Cols.TITLE, group.getTitle());
        values.put(GroupTable.Cols.UUID, group.getId().toString());
        values.put(GroupTable.Cols.LINK, group.getLink());
        values.put(GroupTable.Cols.FACULTY, group.getFaculty());

        return values;
    }

    private GroupCursorWrapper queryGroup(String whereClause, String[] whereArgs) {
        Cursor cursor = mDatabase.query(
                DisciplineTable.NAME,
                null,
                whereClause,
                whereArgs,
                null,
                null,
                null
        );
        return new GroupCursorWrapper(cursor);
    }

    public List<Group> getGroup() {
        List<Group> group = new ArrayList<>();
        GroupCursorWrapper cursor = queryGroup(null, null);
        try {
            cursor.moveToFirst();
            while(!cursor.isAfterLast()) {
                group.add(cursor.getGroup());
                cursor.moveToNext();
            }
        } finally {
            cursor.close();
        }
        return group;
    }

    private GroupStorage(Context context) {
        mContext = context.getApplicationContext();
        mDatabase = new GroupBaseHelper(mContext).getWritableDatabase();
    }

    public Group getGroup(UUID id) {
        GroupCursorWrapper cursor = queryGroup(DisciplineTable.Cols.UUID + " = ?",
                new String[]{id.toString()}
        );
        try {
            cursor.moveToFirst();
            if(cursor.getCount() == 0) return null;
            return cursor.getGroup();
        } finally {
            cursor.close();
        }
    }


    public void addGroup(Group group) {
        ContentValues values = getContentValues(group);
        mDatabase.insert(DisciplineTable.NAME, null, values);
    }

    public void updateDiscipline(Group group) {
        ContentValues values = getContentValues(group);
        mDatabase.update(DisciplineTable.NAME, values, DisciplineTable.Cols.UUID + " = ?",
                new String[]{group.getId().toString()}
        );
    }

    public void deleteDiscipline(Group group) {
        mDatabase.delete(DisciplineTable.NAME, DisciplineTable.Cols.UUID + " = ?",
                new String[]{group.getId().toString()}
        );
    }


    public void resetDb() {
        Log.d("DB", "RESET DB!");
        mDatabase.delete(DisciplineTable.NAME, null, null);
        mDatabase.execSQL("DELETE FROM SQLITE_SEQUENCE WHERE NAME = '" + GroupTable.NAME + "'");

    }
}
