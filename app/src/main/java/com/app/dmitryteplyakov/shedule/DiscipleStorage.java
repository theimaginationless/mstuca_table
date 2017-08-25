package com.app.dmitryteplyakov.shedule;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by Dmitry on 20.07.2017.
 */

public class DiscipleStorage {
    private static DiscipleStorage sDiscipleStorage;
    private List<Disciple> mDisciples;

    public static DiscipleStorage get(Context context) {
        if(sDiscipleStorage == null)    sDiscipleStorage = new DiscipleStorage(context);
        return sDiscipleStorage;
    }

    public List<Disciple> getDisciples() {
        return mDisciples;
    }

    private DiscipleStorage(Context context) {
        mDisciples = new ArrayList<>();
    }

    public Disciple getDisciple(UUID id) {
        for(Disciple disciple : mDisciples)
            if(disciple.getId().equals(id))
                return disciple;
        return null;
    }

    public void addDisciple(Disciple d) {
        mDisciples.add(d);
    }
}
