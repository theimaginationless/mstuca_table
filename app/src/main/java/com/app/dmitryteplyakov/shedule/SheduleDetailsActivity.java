package com.app.dmitryteplyakov.shedule;

import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Created by Dmitry on 20.07.2017.
 */

public class SheduleDetailsActivity extends CommonFragmentActivity {

    protected Fragment createFragment() {
        return new SheduleDetailsFragment();
    }


}
