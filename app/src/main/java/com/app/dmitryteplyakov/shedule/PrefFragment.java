package com.app.dmitryteplyakov.shedule;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceManager;
import android.util.Log;

import com.app.dmitryteplyakov.shedule.Core.DisciplineStorage;

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
                ListPreference prefListlang = (ListPreference) findPreference("subgroup_lang");
                ListPreference prefListLab = (ListPreference) findPreference("subgroup_lab");
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
                final String firstListLangValue = sharedPreferences.getString("subgroup_lang", getString(R.string.first));
                final String firstListLabValue = sharedPreferences.getString("subgroup_lab", getString(R.string.first));

                prefListlang.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newVal) {
                        if(!firstListLangValue.equals((String) newVal)) {
                            Log.d("Pref", "Subgroup is changed. Drop DB!");
                            DisciplineStorage.get(getActivity()).resetDb();
                        }
                        return true;
                    }
                });
                prefListLab.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newVal) {
                        if(!firstListLabValue.equals((String) newVal)) {
                            Log.d("Pref", "Subgroup is changed. Drop DB!");
                            DisciplineStorage.get(getActivity()).resetDb();
                        }
                        return true;
                    }
                });
            }
        }
    }
}
