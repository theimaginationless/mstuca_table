package com.app.dmitryteplyakov.shedule.database;

import android.database.Cursor;
import android.database.CursorWrapper;

import com.app.dmitryteplyakov.shedule.Core.Discipline;

import java.util.Date;
import java.util.UUID;

import static com.app.dmitryteplyakov.shedule.database.DisciplineDbSchema.*;

/**
 * Created by dmitry21 on 25.08.17.
 */

public class DisciplineCursorWrapper extends CursorWrapper {
    public DisciplineCursorWrapper(Cursor cursor) {
        super(cursor);
    }

    public Discipline getDiscipline() {
        String uuidString = getString(getColumnIndex(DisciplineTable.Cols.UUID));
        String titleString = getString(getColumnIndex(DisciplineTable.Cols.DISCIPLINETITLE));
        String teacherString = getString(getColumnIndex(DisciplineTable.Cols.TEACHERNAME));
        String audString = getString(getColumnIndex(DisciplineTable.Cols.AUDNUMBER));
        long dateLong = getLong(getColumnIndex(DisciplineTable.Cols.DATE));
        String type = getString(getColumnIndex(DisciplineTable.Cols.TYPE));
        int numberInt = getInt(getColumnIndex(DisciplineTable.Cols.NUMBER));

        Discipline discipline = new Discipline();
        discipline.setId(UUID.fromString(uuidString));
        discipline.setTeacherName(teacherString);
        discipline.setDiscipleName(titleString);
        discipline.setAuditoryNumber(audString);
        discipline.setDate(new Date(dateLong));
        discipline.setType(type);
        discipline.setNumber(numberInt);

        return discipline;
    }
}
