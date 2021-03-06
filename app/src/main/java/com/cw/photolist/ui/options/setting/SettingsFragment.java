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
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;

import androidx.preference.PreferenceFragment;
import androidx.leanback.preference.LeanbackPreferenceFragment;
import androidx.leanback.preference.LeanbackSettingsFragment;
import androidx.preference.DialogPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.cw.photolist.utility.Pref;
import com.cw.photolist.R;
import com.cw.photolist.data.DbHelper;
import com.cw.photolist.data.PhotoContract;
import com.cw.photolist.data.PhotoProvider;
import com.cw.photolist.define.Define;
import com.cw.photolist.ui.MainActivity;
import com.cw.photolist.ui.MainFragment;

import java.util.Objects;

import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static com.cw.photolist.define.Define.DEFAULT_AUTO_PLAY_BY_CATEGORY;
import static com.cw.photolist.define.Define.DEFAULT_AUTO_PLAY_BY_LIST;

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
        }

        @Override
        public boolean onPreferenceTreeClick(Preference preference) {
//            if (preference.getKey().equals(getString(R.string.pref_key_login))) {
//                // Open an AuthenticationActivity
//                startActivity(new Intent(getActivity(), AuthenticationActivity.class));
//            }
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(act);
            SharedPreferences.Editor sharedPreferencesEditor = sharedPreferences.edit();

            if (preference.getKey().equals(getString(R.string.pref_key_auto_play_by_list))){
                if (preference.getKey().equals(getString(R.string.pref_key_auto_play_by_list))){
                    boolean currentSetting = sharedPreferences.getBoolean(
                                    act.getString(R.string.pref_key_auto_play_by_list),
                                    DEFAULT_AUTO_PLAY_BY_LIST);

                    // keep one auto play mode
                    if(currentSetting) {
                        sharedPreferencesEditor.putBoolean(
                                getString(R.string.pref_key_auto_play_by_category),
                                false);
                        sharedPreferencesEditor.apply();
                    }
                }

                startNewMainAct();
            }

            if (preference.getKey().equals(getString(R.string.pref_key_auto_play_by_category))) {
                if (preference.getKey().equals(getString(R.string.pref_key_auto_play_by_category))){
                    boolean currentSetting = sharedPreferences.getBoolean(
                            act.getString(R.string.pref_key_auto_play_by_category),
                            DEFAULT_AUTO_PLAY_BY_CATEGORY);
                    // keep one auto play mode
                    if(currentSetting) {
                        sharedPreferencesEditor.putBoolean(
                                getString(R.string.pref_key_auto_play_by_list),
                                false);
                        sharedPreferencesEditor.apply();
                    }
                }
                startNewMainAct();
            }

            if (preference.getKey().equals(getString(R.string.pref_key_set_default))) {
                Pref.setPref_link_source_number(act, Define.INIT_SOURCE_LINK_NUMBER);
                startRenewFetchService();

                // remove category name key
                Pref.removePref_category_name(act);

                MainFragment.mCategoryNames = null;

                startNewMainAct();
            }

            // remove invalid links
            if (preference.getKey().equals(getString(R.string.pref_key_remove_invalid_links))){
            }

            return super.onPreferenceTreeClick(preference);
        }

        // start new main activity
        void startNewMainAct(){
            // start new MainActivity to refresh card view
            Intent new_intent = new Intent(act, MainActivity.class);
            new_intent.addFlags(FLAG_ACTIVITY_CLEAR_TASK);
            new_intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
            Objects.requireNonNull(act).startActivity(new_intent);
        }

        // start fetch service by URL string
        private void startRenewFetchService() {
            System.out.println("SelectLinkSrcFragment / _startFetchService");
            // delete database
            try {
                System.out.println("SelectLinkSrcFragment / _startFetchService / will delete DB");
                Objects.requireNonNull(act).deleteDatabase(DbHelper.DATABASE_NAME);

                ContentResolver resolver = act.getContentResolver();
                ContentProviderClient client = resolver.acquireContentProviderClient(PhotoContract.CONTENT_AUTHORITY);
                assert client != null;
                PhotoProvider provider = (PhotoProvider) client.getLocalContentProvider();

                assert provider != null;
                provider.mContentResolver = resolver;
                provider.mOpenHelper.close();

                provider.mOpenHelper = new DbHelper(act);
                provider.mOpenHelper.setWriteAheadLoggingEnabled(false);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                    client.close();
                else
                    client.release();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }
}