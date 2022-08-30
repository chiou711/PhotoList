/*
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.cw.photolist.ui.options.setting;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

import androidx.preference.ListPreference;
import androidx.preference.PreferenceFragment;
import androidx.leanback.preference.LeanbackPreferenceFragment;
import androidx.leanback.preference.LeanbackSettingsFragment;
import androidx.preference.DialogPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import com.cw.photolist.define.Define;
import com.cw.photolist.ui.ScanLocalAct;
import com.cw.photolist.R;
import com.cw.photolist.ui.MainActivity;

import java.util.Objects;

import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static com.cw.photolist.define.Define.DEFAULT_AUTO_PLAY;

public class SettingsFragment extends LeanbackSettingsFragment
        implements DialogPreference.TargetFragment {
    private final static String PREFERENCE_RESOURCE_ID = "preferenceResource";
    private final static String PREFERENCE_ROOT = "root";
    private PreferenceFragment mPreferenceFragment;

    @Override
    public void onPreferenceStartInitialScreen() {
        mPreferenceFragment = buildPreferenceFragment(R.xml.settings, null);
        startPreferenceFragment(mPreferenceFragment);
    }

    @Override
    public boolean onPreferenceStartFragment(PreferenceFragment preferenceFragment,
        Preference preference) {
        return false;
    }

    @Override
    public boolean onPreferenceStartScreen(PreferenceFragment preferenceFragment,
        PreferenceScreen preferenceScreen) {
        PreferenceFragment frag = buildPreferenceFragment(R.xml.settings,
            preferenceScreen.getKey());
        startPreferenceFragment(frag);
        return true;
    }

    @Override
    public Preference findPreference(CharSequence charSequence) {
        return mPreferenceFragment.findPreference(charSequence);
    }

    private PreferenceFragment buildPreferenceFragment(int preferenceResId, String root) {
        PreferenceFragment fragment = new PrefFragment();
        Bundle args = new Bundle();
        args.putInt(PREFERENCE_RESOURCE_ID, preferenceResId);
        args.putString(PREFERENCE_ROOT, root);
        fragment.setArguments(args);
        return fragment;
    }

    public static class PrefFragment extends LeanbackPreferenceFragment {
        Activity act;

        @Override
        public void onCreatePreferences(Bundle bundle, String s) {
            String root = getArguments().getString(PREFERENCE_ROOT, null);
            int prefResId = getArguments().getInt(PREFERENCE_RESOURCE_ID);
            if (root == null) {
                addPreferencesFromResource(prefResId);
            } else {
                setPreferencesFromResource(prefResId, root);
            }

            act = getActivity();

            showRangeTitle();
            showDurationTitle();
        }

        // show range title
        void showRangeTitle(){
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(act);

            String currRangeSetting = sharedPreferences.getString(
                    getString(R.string.pref_key_cyclic_play_range),
                    "0" );

            ListPreference range_selection =  (ListPreference)findPreference(getString(R.string.pref_key_cyclic_play_range));
            String oriTitle = (String) getString(R.string.pref_title_range_selection);
            String actualTitle;

            if(currRangeSetting.equalsIgnoreCase("0"))
                actualTitle  = oriTitle.concat(" : ").concat(getString(R.string.pref_title_cyclic_play_by_category));
            else
                actualTitle  = oriTitle.concat(" : ").concat(getString(R.string.pref_title_cyclic_play_by_list));

            range_selection.setTitle(actualTitle);
        }

        // show display duration
        void showDurationTitle(){
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(act);

            String currDurationSetting = sharedPreferences.getString(
                    getString(R.string.pref_key_auto_play_duration),
                    String.valueOf(Define.DEFAULT_DISPLAY_DURATION ));

            ListPreference duration_selection =  (ListPreference)findPreference(getString(R.string.pref_key_auto_play_duration));
            String oriTitle = (String) getString(R.string.pref_title_auto_play_duration);
            String actualTitle = null;

            // get current duration
            String[] entries = getResources().getStringArray(R.array.duration_range_entries);
            String[] values = getResources().getStringArray(R.array.duration_range_entry_values);
            for(int i=0;i<values.length;i++){
                if(currDurationSetting.equals(values[i]))
                    actualTitle  = oriTitle.concat(" : ").concat(entries[i]);
            }

            // set title
            duration_selection.setTitle(actualTitle);
        }

        @Override
        public boolean onPreferenceTreeClick(Preference preference) {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(act);
            SharedPreferences.Editor sharedPreferencesEditor = sharedPreferences.edit();

            // select cyclic range
            if (preference.getKey().equals(getString(R.string.pref_key_cyclic_play_range))){

                ListPreference range_selection =  (ListPreference)findPreference(getString(R.string.pref_key_cyclic_play_range));

                // Listener for list preference
                range_selection.setOnPreferenceChangeListener((preference1, newValue) -> {
                    SharedPreferences sharedPreferences1 = PreferenceManager.getDefaultSharedPreferences(act);
                    SharedPreferences.Editor sharedPreferencesEditor1 = sharedPreferences1.edit();

                    // apply selection range
                    sharedPreferencesEditor1.putString(
                            getString(R.string.pref_key_cyclic_play_range),
                            (String)newValue);

                    sharedPreferencesEditor1.apply();

                    showRangeTitle();
                    return false;
                });

                String currRangeSetting = sharedPreferences.getString(
                        getString(R.string.pref_key_cyclic_play_range),
                        "0" );

                if(currRangeSetting.equalsIgnoreCase("0"))
                    range_selection.setValueIndex(0);
                else
                    range_selection.setValueIndex(1);
            }

            // set auto play
            if (preference.getKey().equals(getString(R.string.pref_key_auto_play_switch))) {
                if (preference.getKey().equals(getString(R.string.pref_key_auto_play_switch))){
                    boolean currentSetting = sharedPreferences.getBoolean(
                            getString(R.string.pref_key_auto_play_switch),
                            DEFAULT_AUTO_PLAY);

                    SwitchPreference sw =(SwitchPreference)findPreference(getString(R.string.pref_key_auto_play_switch));
                    sw.setChecked(currentSetting);

                    // Listener for switch preference
                    sw.setOnPreferenceChangeListener((preference12, newValue) -> {
                        // toggle auto play
                        sharedPreferencesEditor.putBoolean(
                                getString(R.string.pref_key_auto_play_switch),
                                (boolean)newValue);
                        sharedPreferencesEditor.apply();

                        return true;
                    });
                }
            }

            // select auto play duration
            if (preference.getKey().equals(getString(R.string.pref_key_auto_play_duration))){

                ListPreference duration_selection =  (ListPreference)findPreference(getString(R.string.pref_key_auto_play_duration));

                // Listener for list preference
                duration_selection.setOnPreferenceChangeListener((preference1, newValue) -> {
                    SharedPreferences sharedPreferences1 = PreferenceManager.getDefaultSharedPreferences(act);
                    SharedPreferences.Editor sharedPreferencesEditor1 = sharedPreferences1.edit();

                    // apply selection range
                    sharedPreferencesEditor1.putString(
                            getString(R.string.pref_key_auto_play_duration),
                            (String)newValue);

                    sharedPreferencesEditor1.apply();

                    showDurationTitle();

                    return false;
                });

                String currRangeSetting = sharedPreferences.getString(
                        getString(R.string.pref_key_auto_play_duration),
                        String.valueOf(Define.DEFAULT_DISPLAY_DURATION ));

                // highlight current option
                String[] listDuration;
                listDuration = getResources().getStringArray(R.array.duration_range_entry_values);
                for(int i=0;i<listDuration.length;i++){
                    if(currRangeSetting.equals(listDuration[i]))
                        duration_selection.setValueIndex(i);
                }
            }

            // create DB
            if (preference.getKey().equals(getString(R.string.pref_key_db_is_created))) {
                Intent intent = new Intent(getActivity(), ScanLocalAct.class);
                startActivity(intent);
            }

            return super.onPreferenceTreeClick(preference);
        }

    }
}