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
                final ListPreference prefListlang = (ListPreference) findPreference("subgroup_lang");
                final ListPreference prefListLab = (ListPreference) findPreference("subgroup_lab");
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
                final String firstListLangValue = sharedPreferences.getString("subgroup_lang", getString(R.string.first));
                final String firstListLabValue = sharedPreferences.getString("subgroup_lab", getString(R.string.first));
                final ListPreference prefListCourse = (ListPreference) findPreference("course");

                prefListCourse.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        DisciplineStorage.get(getActivity()).resetDb();
                        Log.d("Pref", "Subgroup is changed. Drop DB!");
                        SheduleListFragment.setIsCourseChanged(true);
                        return true;
                    }
                });
                /*ListPreference prefListFaculty = (ListPreference) findPreference("faculty");
                final ListPreference prefListSpec = (ListPreference) findPreference("spec");
                final ListPreference prefListStream = (ListPreference) findPreference("stream");
                final ListPreference prefListCourse = (ListPreference) findPreference("course");

                if(!sharedPreferences.getString("spec", "0").equals("0")) {
                    prefListlang.setEnabled(true);
                    prefListLab.setEnabled(true);
                    prefListCourse.setEnabled(true);
                }

                prefListStream.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        prefListlang.setEnabled(true);
                        prefListLab.setEnabled(true);
                        return true;
                    }
                });

                prefListSpec.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        prefListStream.setEnabled(true);
                        Log.d("Pref", "Subgroup is changed. Drop DB!");
                        DisciplineStorage.get(getActivity()).resetDb();
                        prefListCourse.setEnabled(true);
                        return true;

                    }

                });

                prefListFaculty.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        prefListSpec.setEnabled(true);
                        if(((String) newValue).equals(getString(R.string.appmath_and_cs))) {
                            prefListSpec.setEntries(R.array.app_math_and_cs_list);
                            prefListSpec.setEntryValues(R.array.app_math_and_cs_list_values);
                        }
                        Log.d("Pref", "Subgroup is changed. Drop DB!");
                        DisciplineStorage.get(getActivity()).resetDb();
                        else if(((String) newValue).equals(getString(R.string.mech))) {
                            prefListSpec.setEntries(R.array.mech_list);
                            prefListSpec.setEntryValues(R.array.mech_list);
                        }
                        else if(((String) newValue).equals(getString(R.string.fask))) {
                            prefListSpec.setEntries(R.array.fask_list);
                            prefListSpec.setEntryValues(R.array.fask_list);
                        }
                        else if(((String) newValue).equals(getString(R.string.fuvt))) {
                            prefListSpec.setEntries(R.array.fuvt_list);
                            prefListSpec.setEntryValues(R.array.fuvt_list);
                        }

                        return true;
                    }
                });
        */

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
