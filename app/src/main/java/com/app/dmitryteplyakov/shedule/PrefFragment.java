package com.app.dmitryteplyakov.shedule;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.SwitchPreference;
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
                ListPreference prefListFaculty = (ListPreference) findPreference("faculty");
                final ListPreference prefListSpec = (ListPreference) findPreference("spec");
                final ListPreference prefListStream = (ListPreference) findPreference("stream");

                String specVal = sharedPreferences.getString("faculty", "0");

                if(specVal.equals(getString(R.string.appmath_and_cs))) {
                    prefListSpec.setEntries(R.array.app_math_and_cs_list);
                    prefListSpec.setEntryValues(R.array.app_math_and_cs_list_values);
                }
                else if(specVal.equals(getString(R.string.mech_link))) {
                    prefListSpec.setEntries(R.array.mech_list);
                    prefListSpec.setEntryValues(R.array.mech_list_values);
                }
                else if(specVal.equals(getString(R.string.fask))) {
                    prefListSpec.setEntries(R.array.fask_list);
                    prefListSpec.setEntryValues(R.array.fask_list_values);
                }
                else if(specVal.equals(getString(R.string.fuvt))) {
                    prefListSpec.setEntries(R.array.fuvt_list);
                    prefListSpec.setEntryValues(R.array.fuvt_list_values);
                }

                prefListCourse.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        DisciplineStorage.get(getActivity()).resetDb();
                        Log.d("Pref", "Drop DB! Count: " + Integer.toString(DisciplineStorage.get(getActivity()).getDisciplines().size()));
                        SheduleListFragment.setIsCourseChanged(true);
                        SheduleListFragment.setNeedUpdate(true);
                        return true;
                    }
                });


                if(!sharedPreferences.getString("course", "0").equals("0")) {
                    prefListCourse.setEnabled(true);
                    prefListlang.setEnabled(true);
                    prefListLab.setEnabled(true);
                    prefListStream.setEnabled(true);
                    prefListSpec.setEnabled(true);
                }

                prefListStream.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        prefListlang.setEnabled(true);
                        prefListLab.setEnabled(true);
                        prefListCourse.setEnabled(true);
                        DisciplineStorage.get(getActivity()).resetDb();
                        Log.d("Pref", "Drop DB! Count: " + Integer.toString(DisciplineStorage.get(getActivity()).getDisciplines().size()));
                        SheduleListFragment.setIsCourseChanged(true);
                        SheduleListFragment.setNeedUpdate(true);
                        return true;
                    }
                });

                prefListSpec.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        prefListStream.setEnabled(true);
                        DisciplineStorage.get(getActivity()).resetDb();
                        Log.d("Pref", "Drop DB! Count: " + Integer.toString(DisciplineStorage.get(getActivity()).getDisciplines().size()));
                        SheduleListFragment.setIsCourseChanged(true);
                        SheduleListFragment.setNeedUpdate(true);
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
                        else if(((String) newValue).equals(getString(R.string.mech_link))) {
                            prefListSpec.setEntries(R.array.mech_list);
                            prefListSpec.setEntryValues(R.array.mech_list_values);
                        }
                        else if(((String) newValue).equals(getString(R.string.fask))) {
                            prefListSpec.setEntries(R.array.fask_list);
                            prefListSpec.setEntryValues(R.array.fask_list_values);
                        }
                        else if(((String) newValue).equals(getString(R.string.fuvt))) {
                            prefListSpec.setEntries(R.array.fuvt_list);
                            prefListSpec.setEntryValues(R.array.fuvt_list_values);
                        }
                        DisciplineStorage.get(getActivity()).resetDb();
                        Log.d("Pref", "Drop DB! Count: " + Integer.toString(DisciplineStorage.get(getActivity()).getDisciplines().size()));
                        SheduleListFragment.setIsCourseChanged(true);
                        SheduleListFragment.setNeedUpdate(true);
                        return true;
                    }
                });

                prefListCourse.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        prefListLab.setEnabled(true);
                        prefListlang.setEnabled(true);
                        DisciplineStorage.get(getActivity()).resetDb();
                        Log.d("Pref", "Drop DB! Count: " + Integer.toString(DisciplineStorage.get(getActivity()).getDisciplines().size()));
                        SheduleListFragment.setIsCourseChanged(true);
                        SheduleListFragment.setResetPosition(true);
                        SheduleListFragment.setNeedUpdate(true);
                        return true;
                    }
                });

                prefListlang.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newVal) {
                        if(!firstListLangValue.equals((String) newVal)) {
                            DisciplineStorage.get(getActivity()).resetDb();
                            Log.d("Pref", "Drop DB! Count: " + Integer.toString(DisciplineStorage.get(getActivity()).getDisciplines().size()));
                            SheduleListFragment.setNeedUpdate(true);
                        }
                        return true;
                    }
                });
                prefListLab.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newVal) {
                        if(!firstListLabValue.equals((String) newVal)) {
                            DisciplineStorage.get(getActivity()).resetDb();
                            Log.d("Pref", "SDrop DB! Count: " + Integer.toString(DisciplineStorage.get(getActivity()).getDisciplines().size()));
                            SheduleListFragment.setNeedUpdate(true);
                        }
                        return true;
                    }
                });
            }
        }
    }
}
