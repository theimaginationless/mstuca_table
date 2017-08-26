package com.app.dmitryteplyakov.shedule;

import android.os.Bundle;
import android.support.v7.preference.PreferenceFragmentCompat;

/**
 * Created by dmitry21 on 26.08.17.
 */

public class PrefFragment extends PreferenceFragmentCompat {
    private static final String ARG_PREF = "com.app.preffragment.arg_pref";

    public static PrefFragment newInstance(String sett) {
        Bundle args = new Bundle();
        args.putSerializable(ARG_PREF, sett);
        PrefFragment fragment = new PrefFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String s) {
        if(getArguments().getSerializable(ARG_PREF) != null) {
            String sett = (String) getArguments().getSerializable(ARG_PREF);
            if(sett.equals("general")) {
                addPreferencesFromResource(R.xml.preferences);
            }
        }
    }
}
