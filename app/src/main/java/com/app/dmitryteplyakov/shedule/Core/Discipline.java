package com.app.dmitryteplyakov.shedule.Core;

import java.util.Comparator;
import java.util.Date;
import java.util.UUID;

/**
 * Created by Dmitry on 20.07.2017.
 */

public class Discipline implements Comparable<Discipline> {
    private String mTeacherName;
    private String mDiscipleName;
    private String mAuditoryNumber;
    private String mType;
    private Date mDate;
    private UUID mId;
    private int mNumber;
    private long academicHourDuraton = 45 * 2 * 60 * 1000;

    public static Comparator<Discipline> numberComparator = new Comparator<Discipline>() {
        @Override
        public int compare(Discipline d1, Discipline d2) {
            return d1.getNumber() - d2.getNumber();
        }
    };

    public static Comparator<Discipline> dateComparator = new Comparator<Discipline>() {
        @Override
        public int compare(Discipline discipline, Discipline t1) {
            if (discipline.getDate().before(t1.getDate()))
                return -1;
            return 1;
        }
    };

    public long getEndTime() {
        return mDate.getTime() + academicHourDuraton;
    }

    public Discipline() {
        mDate = new Date();
        mId = UUID.randomUUID();
    }

    public void setId(UUID id) {
        mId = id;
    }

    public int getNumber() {
        return mNumber;
    }

    public void setNumber(int number) {
        mNumber = number;
    }

    public String getTeacherName() {
        return mTeacherName;
    }

    public void setTeacherName(String teacherName) {
        mTeacherName = teacherName;
    }

    public String getDiscipleName() {
        return mDiscipleName;
    }

    public void setDiscipleName(String discipleName) {
        mDiscipleName = discipleName;
    }

    public String getAuditoryNumber() {
        return mAuditoryNumber;
    }

    public void setAuditoryNumber(String auditoryNumber) {
        mAuditoryNumber = auditoryNumber;
    }

    public String getType() {
        return mType;
    }

    public void setType(String type) {
        mType = type;
    }

    public Date getDate() {
        return mDate;
    }

    public void setDate(Date date) {
        mDate = date;
    }

    public UUID getId() {
        return mId;
    }

    @Override
    public int compareTo(Discipline discipline) {
        if(this.getNumber() < discipline.getNumber())
            return -1;
        return 1;
    }

}
