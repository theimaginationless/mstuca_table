package com.app.dmitryteplyakov.shedule;

import java.util.Date;
import java.util.UUID;

/**
 * Created by Dmitry on 20.07.2017.
 */

public class Disciple {
    private String mTeacherName;
    private String mDiscipleName;
    private String mAuditoryNumber;
    private String LectureType;
    private Date mDate;
    private UUID mId;

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

    public String getLectureType() {
        return LectureType;
    }

    public void setLectureType(String lectureType) {
        LectureType = lectureType;
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
}
