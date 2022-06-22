/*
 * Copyright (C) 2016 The Android Open Source Project
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
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.Toast;

import androidx.preference.PreferenceFragment;
import androidx.leanback.preference.LeanbackPreferenceFragment;
import androidx.leanback.preference.LeanbackSettingsFragment;
import androidx.preference.DialogPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.cw.photolist.BuildConfig;
import com.cw.photolist.R;
import com.cw.photolist.util.Utils;
import com.cw.photolist.data.DbHelper;
import com.cw.photolist.data.VideoContract;
import com.cw.photolist.data.VideoProvider;
import com.cw.photolist.define.Define;
import com.cw.photolist.ui.MainActivity;
import com.cw.photolist.ui.MainFragment;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoListResponse;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

            if (preference.getKey().equals(getString(R.string.pref_key_show_duration))) {
                // start new MainActivity to refresh card view
                startNewMainAct();
            }

            if (preference.getKey().equals(getString(R.string.pref_key_set_default))) {
                Utils.setPref_link_source_number(act, Define.INIT_SOURCE_LINK_NUMBER);
                startRenewFetchService();

                // remove category name key
                Utils.removePref_category_name(act);

                MainFragment.mCategoryNames = null;

                startNewMainAct();
            }

            // remove invalid links
            if (preference.getKey().equals(getString(R.string.pref_key_remove_invalid_links))) {
                Toast.makeText(act,R.string.please_wait,Toast.LENGTH_LONG).show();

                ExecutorService myExecutor = Executors.newCachedThreadPool();
                myExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    ContentResolver contentResolver = act.getApplicationContext().getContentResolver();
                    DbHelper mOpenHelper = new DbHelper(act);
                    mOpenHelper.setWriteAheadLoggingEnabled(false);

                    // query current video table
                    // check if playlist still exists
                    Cursor cursor = null;
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        // Call requires API level 26(8.0)
                        cursor = contentResolver.query(VideoContract.VideoEntry.CONTENT_URI, null,null,null);
                    }
                    int videoLinksCount = cursor.getCount();

                    for(int i=0;i<videoLinksCount;i++) {

                        // get link by video ID
                        cursor.moveToPosition(i);

                        // get video ID
                        int video_id_index = cursor.getColumnIndex(VideoContract.VideoEntry._ID);
                        long video_id = cursor.getLong(video_id_index);

                        // get link URL
                        int linkUrl_index = cursor.getColumnIndex(VideoContract.VideoEntry.COLUMN_LINK_URL);
                        String linkUrl = cursor.getString(linkUrl_index);

                        // check if URL is working
                        YouTube youtube = new YouTube.Builder(new NetHttpTransport(), new JacksonFactory(),
                                new HttpRequestInitializer() {
                                    public void initialize(HttpRequest request) throws IOException {
                                    }
                                }).setApplicationName(act.getString(R.string.app_name)).build();

                        String videoId = Utils.getYoutubeId(linkUrl);
                        YouTube.Videos.List videoRequest = null;
                        try {
//                            videoRequest = youtube.videos().list("snippet,statistics,contentDetails");
                            videoRequest = youtube.videos().list("snippet");
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        videoRequest.setId(videoId);

                        //get the key/values from the meta-data in AndroidManifest
                        ApplicationInfo ai;
                        String developer_key = null;
                        String sha_1 = null;
                        try {
                            ai = getActivity().getPackageManager()
                                    .getApplicationInfo(getActivity().getPackageName(), PackageManager.GET_META_DATA);
                            developer_key = ai.metaData.get("key_DEVELOPER_KEY").toString();
                            sha_1 = ai.metaData.get("key_SHA1").toString();
                        } catch (PackageManager.NameNotFoundException e) {
                            e.printStackTrace();
                        }
                        videoRequest.setKey(developer_key);

                        // set http headers for restricting Android App
                        HttpHeaders httpHeaders = new HttpHeaders();
                        httpHeaders.set("Content-Type","application/json");
                        httpHeaders.set("X-Android-Package", BuildConfig.APPLICATION_ID);
                        httpHeaders.set("X-Android-Cert",sha_1);
                        videoRequest.setRequestHeaders(httpHeaders);

                        VideoListResponse listResponse = null;
                        try {
                            listResponse = videoRequest.execute();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        List<Video> videoList = listResponse.getItems();

                        // if response is NG
                        if(videoList.size()== 0) {
                            int tableId = Utils.getPref_video_table_id(act);
                            // delete item
                            mOpenHelper.getWritableDatabase().delete(
                                    VideoContract.VideoEntry.TABLE_NAME.concat(String.valueOf(tableId)),
                                            "_id=".concat(String.valueOf(video_id)),
                                            null);
                        }
                    }
                    cursor.close();

                    startNewMainAct();
                }//_run
                });
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
                ContentProviderClient client = resolver.acquireContentProviderClient(VideoContract.CONTENT_AUTHORITY);
                assert client != null;
                VideoProvider provider = (VideoProvider) client.getLocalContentProvider();

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