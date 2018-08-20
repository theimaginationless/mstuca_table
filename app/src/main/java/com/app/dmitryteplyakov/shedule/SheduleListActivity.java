package com.app.dmitryteplyakov.shedule;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class SheduleListActivity extends CommonFragmentActivity {

    protected Fragment createFragment() {
        return new SheduleListFragment();
    }
}
