package com.app.dmitryteplyakov.shedule.Core.Group;

import java.util.Comparator;
import java.util.Date;
import java.util.UUID;

/**
 * Created by Dmitry on 20.07.2017.
 */

public class Group {
    private String mTitle;
    private String mFaculty;
    private String mLink;
    private UUID mId;

    public String getTitle() {
        return mTitle;
    }

    public void setTitle(String title) {
        mTitle = title;
    }

    public String getFaculty() {
        return mFaculty;
    }

    public void setFaculty(String faculty) {
        mFaculty = faculty;
    }

    public String getLink() {
        return mLink;
    }

    public void setLink(String link) {
        mLink = link;
    }

    public UUID getId() {
        return mId;
    }

    public void setId(UUID id) {
        mId = id;
    }
}
