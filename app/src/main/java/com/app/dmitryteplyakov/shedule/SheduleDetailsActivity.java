package com.app.dmitryteplyakov.shedule;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;

/**
 * Created by Dmitry on 20.07.2017.
 */

public class SheduleDetailsActivity extends CommonFragmentActivity {

    protected Fragment createFragment() {
        return new SheduleDetailsFragment();
    }
}
