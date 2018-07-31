package com.app.dmitryteplyakov.shedule.database.Group;

import android.database.Cursor;
import android.database.CursorWrapper;

import com.app.dmitryteplyakov.shedule.Core.Discipline;
import com.app.dmitryteplyakov.shedule.Core.Group.Group;

import java.util.Date;
import java.util.UUID;

import static com.app.dmitryteplyakov.shedule.database.DisciplineDbSchema.DisciplineTable;
import static com.app.dmitryteplyakov.shedule.database.Group.GroupDbSchema.*;

/**
 * Created by dmitry21 on 25.08.17.
 */

public class GroupCursorWrapper extends CursorWrapper {
    public GroupCursorWrapper(Cursor cursor) {
        super(cursor);
    }

    public Group getGroup() {
        String uuidString = getString(getColumnIndex(GroupTable.Cols.UUID));
        String titleString = getString(getColumnIndex(GroupTable.Cols.TITLE));
        String facultyString = getString(getColumnIndex(GroupTable.Cols.FACULTY));
        String linkString = getString(getColumnIndex(GroupTable.Cols.LINK));

        Group group = new Group();
        group.setId(UUID.fromString(uuidString));
        group.setFaculty(facultyString);
        group.setTitle(titleString);
        group.setLink(linkString);

        return group;
    }
}
