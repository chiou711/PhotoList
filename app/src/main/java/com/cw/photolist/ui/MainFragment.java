/*
 * Copyright (c) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cw.photolist.ui;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;
import androidx.leanback.app.BackgroundManager;
import androidx.leanback.app.BrowseSupportFragment;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.CursorObjectAdapter;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.OnItemViewSelectedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.PresenterSelector;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;
import androidx.core.app.ActivityOptionsCompat;
import androidx.loader.app.LoaderManager;
import androidx.core.content.ContextCompat;
import androidx.loader.content.Loader;
import androidx.loader.content.CursorLoader;

import android.util.DisplayMetrics;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.BaseInputConnection;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.cw.photolist.utility.Pref;
import com.cw.photolist.R;
import com.cw.photolist.data.DbData;
import com.cw.photolist.ui.photo.AutoPhotoAct;
import com.cw.photolist.ui.photo.ManualPhotoFragment;
import com.cw.photolist.utility.StorageUtils;
import com.cw.photolist.utility.Utils;
import com.cw.photolist.data.DbHelper;
import com.cw.photolist.data.Pair;
import com.cw.photolist.data.Source_links;
import com.cw.photolist.data.PhotoContract;
import com.cw.photolist.define.Define;
import com.cw.photolist.model.Video;
import com.cw.photolist.presenter.CardPresenter;
import com.cw.photolist.model.VideoCursorMapper;
import com.cw.photolist.presenter.GridItemPresenter;
import com.cw.photolist.presenter.IconHeaderItemPresenter;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.cw.photolist.define.Define.INIT_CATEGORY_NUMBER;
import static com.cw.photolist.utility.Utils.PERMISSIONS_REQUEST_STORAGE;

import com.cw.photolist.ui.options.select_category.SelectCategoryActivity;
import com.cw.photolist.ui.options.setting.SettingsActivity;
import com.cw.photolist.ui.options.browse_category.BrowseCategoryActivity;
import com.cw.photolist.utility.LocalData;

/*
 * Main class to show BrowseFragment with header and rows of videos
 */
public class MainFragment extends BrowseSupportFragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int BACKGROUND_UPDATE_DELAY = 300;
    private final Handler mHandler = new Handler();
    private ArrayObjectAdapter mTitleRowAdapter;
    private Drawable mDefaultBackground;
    private DisplayMetrics mMetrics;
    private Runnable mBackgroundTask;
    private Uri mBackgroundURI;
    private BackgroundManager mBackgroundManager;
    private LoaderManager mLoaderManager;
    private static final int CATEGORY_LOADER = 100; // Unique ID for Category Loader.
    private static final int TITLE_LOADER = 101; // Unique ID for Title Loader.
	public static List<String> mCategoryNames;
    public final static int PHOTO_INTENT = 97;
    public final static int VIDEO_DETAILS_INTENT = 99;
    // Maps a Loader Id to its CursorObjectAdapter.
    private SparseArray<CursorObjectAdapter> mVideoCursorAdapters;

    // loaded rows after Refresh
    private int rowsLoadedCount;

    private FragmentActivity act;
    public static List<RowInfo> rowInfoList;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Define.setAppBuildMode();
        // Release mode: no debug message
        if (Define.CODE_MODE == Define.RELEASE_MODE) {
            OutputStream nullDev = new OutputStream() {
                public void close() {}
                public void flush() {}
                public void write(byte[] b) {}
                public void write(byte[] b, int off, int len) {}
                public void write(int b) {}
            };
            System.setOut(new PrintStream(nullDev));
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        System.out.println("MainFragment / _onAttach");

        // Create a list to contain all the CursorObjectAdapters.
        // Each adapter is used to render a specific row of videos in the MainFragment.
        mVideoCursorAdapters = new SparseArray<>();//new HashMap<>();

        // Start loading the titles from the database.
        mLoaderManager = LoaderManager.getInstance(this);
        mLoaderManager.initLoader(CATEGORY_LOADER, null, this);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        // Final initialization, modifying UI elements.
        super.onActivityCreated(savedInstanceState);

        act = getActivity();

        // Prepare the manager that maintains the same background image between activities.
        prepareBackgroundManager();

        setupUIElements();

        setupEventListeners();

        prepareEntranceTransition();
    }

    AlertDialog.Builder builder;
    private AlertDialog alertDlg;
    private Handler handler;
    private int count;
    private String countStr;
    private String nextLinkTitle;

    public void onResume() {
        super.onResume();

        System.out.println("MainFragment / _onResume");

//        doSeeAll();
    }

    //
    // see all directories
    String appDir;
    String currFilePath;
    void doSeeAll() {
        List<StorageUtils.StorageInfo> storageList = StorageUtils.getStorageList();

        for (int i = 0; i < storageList.size(); i++) {
            System.out.println("-->  storageList[" + i + "] name = " + storageList.get(i).getDisplayName());
            System.out.println("-->  storageList[" + i + "] path = " + storageList.get(i).path);
            System.out.println("-->  storageList[" + i + "] display number = " + storageList.get(i).display_number);

            String sdCardPath = storageList.get(i).path;

            appDir = sdCardPath;

            if (appDir.contains("/mnt/media_rw"))
                appDir = appDir.replace("mnt/media_rw", "storage");

            System.out.println("-->  storageList[" + i + "] appDir = " + appDir);

            currFilePath = appDir;

            LocalData.init(currFilePath);
            LocalData.scan_and_save(act,currFilePath,i);
            List<String> categoryArray = LocalData.category_array;

            // check
            int size = categoryArray.size();
            System.out.println("--------------- size = " + size);
            for (int j = 0; j < size; j++) {
                System.out.println("--------------- categoryArray.get("+j+") = " + categoryArray.get(j));
            }

            LocalData.init(currFilePath);
            LocalData.scan_and_save(act,currFilePath,i);
            List<com.cw.photolist.utility.Photo> photoArray = LocalData.photo_array;

            // check
            size = photoArray.size();
            System.out.println("--------------- size = " + size);
            for(int k=0;k< size; k++ ){
                System.out.println("--------------- list title = " + photoArray.get(k).getList_title() );
                System.out.println("--------------- photo link = " + photoArray.get(k).getPhoto_link() );
            }
        }
        ///
    }

    @Override
    public void onPause() {
        super.onPause();
        System.out.println("MainFragment / _onPause");

        if(alertDlg != null)
            alertDlg.dismiss();
    }

    @Override
    public void onStop() {
        System.out.println("MainFragment / _onStop");
        if(mBackgroundManager!=null)
            mBackgroundManager.release();
        cancelPhotoHandler();
        super.onStop();
    }

    @Override
    public void onDestroy() {
        System.out.println("MainFragment / _onDestroy");

        mHandler.removeCallbacks(mBackgroundTask);
        mBackgroundManager = null;

        super.onDestroy();
    }

    private void prepareBackgroundManager() {
        mBackgroundManager = BackgroundManager.getInstance(act);

        if(!mBackgroundManager.isAttached())
            mBackgroundManager.attach(act.getWindow());

        mDefaultBackground = getResources().getDrawable(R.drawable.default_background, null);
        mBackgroundTask = new UpdateBackgroundTask();
        mMetrics = new DisplayMetrics();
        act.getWindowManager().getDefaultDisplay().getMetrics(mMetrics);
    }

    private void setupUIElements() {

        // option: drawable
        setBadgeDrawable(act.getResources().getDrawable(R.drawable.ic_launcher, null));

        // Badge, when set, takes precedent over title
        // option: title
//        int focusNumber = getPref_focus_category_number(act);
//        String categoryName = Utils.getPref_category_name(act,focusNumber);
        //setTitle(getString(R.string.browse_title));
//        if(!categoryName.equalsIgnoreCase(String.valueOf(focusNumber)))
//            setTitle(categoryName);

        setHeadersState(HEADERS_ENABLED);
        setHeadersTransitionOnBackEnabled(true); //true: focus will return to header, false: will close App

        // Set fastLane (or headers) background color
        setBrandColor(ContextCompat.getColor(act, R.color.fastlane_background));

        // Set search icon color.
//        setSearchAffordanceColor(ContextCompat.getColor(act, R.color.default_background));

        setHeaderPresenterSelector(new PresenterSelector() {
            @Override
            public Presenter getPresenter(Object o) {
                return new IconHeaderItemPresenter(act);
            }
        });
    }

    private void setupEventListeners() {

        // replace original reach action
        setOnSearchClickedListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (!Utils.isGranted_permission_READ_EXTERNAL_STORAGE(act)) {
                    // request permission dialog
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M/*API23*/) {
                        requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                                PERMISSIONS_REQUEST_STORAGE);
                    }
                } else {
                    // new action: browse
                    Intent intent = new Intent(act, BrowseCategoryActivity.class);
                    Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(act).toBundle();
                    startActivity(intent, bundle);
                }
            }
        });

        setOnItemViewClickedListener(new ItemViewClickedListener());
        setOnItemViewSelectedListener(new ItemViewSelectedListener());
    }

    // public static int currentNavPosition;
    // selected is navigated here
    private final class ItemViewSelectedListener implements OnItemViewSelectedListener {
        @Override
        public void onItemSelected(Presenter.ViewHolder itemViewHolder, Object item,
                                   RowPresenter.ViewHolder rowViewHolder, Row row) {

            if(itemViewHolder!= null && itemViewHolder.view != null)
                itemViewHolder.view.setBackgroundColor(getResources().getColor(R.color.selected_background));

            if (item instanceof Video) {
//                System.out.println("---------- onItemSelected / video");
                mBackgroundURI = Uri.parse(((Video) item).bgImageUrl);
                startBackgroundTimer();
            }
            else if (item instanceof String) {
                System.out.println("---------- onItemSelected / category / item = " + item);
//                for(int i=0;i<mCategoryNames.size();i++)
//                {
//                    if(item.toString().equalsIgnoreCase(mCategoryNames.get(i)))
//                    {
//                        currentNavPosition = i;
//                        System.out.println("---------- current navigation position = " + currentNavPosition);
//                    }
//                }

//                int currentNavPosition =  gridRowAdapterCategory.indexOf(item);
//                System.out.println("----------  currentNavPosition = " + currentNavPosition);

                // switch category by onItemViewSelected
//                String cate_name = Utils.getPref_category_name(act);
//                if( !cate_name.equalsIgnoreCase((String)item) && isOkToChangeCategory)
//                    switchCategory(item);
            }
        }
    }

    boolean isOkToChangeCategory;
    // Switch category by category name
    void switchCategory(Object catName) {

        // renew list for Show row number and link number
        rowInfoList = new ArrayList<>();

        String categoryName =  (String) catName;
        // After delay, start switch DB
        new Handler().postDelayed(new Runnable() {
            public void run() {
                isOkToChangeCategory = false;
                if(isCategoryRow(categoryName)) {
                    try {
                        // switch DB
                        Pref.setPref_category_name(Objects.requireNonNull(getContext()), categoryName );
                        mLoaderManager.destroyLoader(TITLE_LOADER);
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                }
            }
        }, 100);
    }

    int currentRowPos;
    int currentRow1stId;
    int currentRowSize;
    int currentRowLastId;
    private final class ItemViewClickedListener implements OnItemViewClickedListener {
        @Override
        public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                                  RowPresenter.ViewHolder rowViewHolder, Row row) {

            // item is video
            if (item instanceof Video) {
                openVideoItem(item);
            } else if (item instanceof String) {
                System.out.println("MainFragment / onItemClicked / item = "+ item);
                // item is Select category
                if (((String) item).contains(getString(R.string.select_category))) {

                    Intent intent = new Intent(act, SelectCategoryActivity.class);
                    Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(act).toBundle();
                    startActivity(intent, bundle);

                // item is Browse category
//                } else if (((String) item).contains(getString(R.string.category_grid_view_title))) {
//                    Intent intent = new Intent(act, BrowseCategoryActivity.class);
//                    Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(act).toBundle();
//                    startActivity(intent, bundle);

                // item is Setting
                } else if(((String) item).contains(getString(R.string.personal_settings))) {
                    Intent intent = new Intent(act, SettingsActivity.class);
                    Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(act).toBundle();
                    startActivity(intent, bundle);

                // item is Category
                } else {
                    // switch category by onItemClicked
                    switchCategory(item);
                }
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        System.out.println("MainFragment / _onActivityResult");

        // API >= 30
//        if(resultCode == RESULT_OK &&
//           requestCode == ACTION_MANAGE_ALL_FILES_ACCESS_REQUEST_CODE) {
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R /*API 30*/) {
//                if (Environment.isExternalStorageManager()) {
//                    // Manage All Permission is granted
//                    LocalData.createCategoryDB_root(getActivity());
//                }
//            }
//        }

        if(requestCode == PHOTO_INTENT) {
            count = Define.DEFAULT_COUNT_DOWN_TIME_TO_PLAY_NEXT; // countdown time to play next
            builder = new AlertDialog.Builder(getContext());

            // prepare next Id
            int nextId_auto = getNextCursorPositionId_auto(getPlayId());
            setNextId_auto(nextId_auto);

            nextLinkTitle =  getNextPhotoTitle();

            countStr = act.getString(R.string.play_countdown)+
                              " " + count + " " +
                              act.getString(R.string.play_time_unit);
            countStr = countStr.replaceFirst("[0-9]",String.valueOf(count));

            builder.setTitle(act.getString(R.string.play_next))
                    .setMessage(act.getString(R.string.play_4_spaces)+ nextLinkTitle +"\n\n" + countStr)
                    .setPositiveButton(act.getString(R.string.play_stop), new DialogInterface.OnClickListener()
                    {
                        // stop
                        @Override
                        public void onClick(DialogInterface dialog1, int which1)
                        {
                            alertDlg.dismiss();
                            cancelPhotoHandler();
                        }
                    })
                    .setNegativeButton(act.getString(R.string.button_continue), new DialogInterface.OnClickListener()
                    {
                        // continue
                        @Override
                        public void onClick(DialogInterface dialog1, int which1)
                        {
                            // launch next intent
                            alertDlg.dismiss();
                            cancelPhotoHandler();

                            startAutoPhotoIntentForResult(getPhotoPosition());
                        }
                    }).
                    setOnCancelListener(new DialogInterface.OnCancelListener(){
                        // cancel
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            alertDlg.dismiss();
                            cancelPhotoHandler();
                        }
                    } );
            alertDlg = builder.create();

            // set listener for selection
            alertDlg.setOnShowListener(new DialogInterface.OnShowListener() {
                @Override
                public void onShow(DialogInterface dlgInterface) {
                    handler = new Handler();
                    handler.postDelayed(runCountDown,1000);

                    // focus
                    Button negative = alertDlg.getButton(AlertDialog.BUTTON_NEGATIVE);
                    negative.setFocusable(true);
                    negative.setFocusableInTouchMode(true);
                    negative.requestFocus();
                }
            });
            alertDlg.show();
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
//        if (id == CATEGORY_LOADER)
//            System.out.println("MainFragment / _onCreateLoader / id = CATEGORY_LOADER");
//        else if(id == TITLE_LOADER)
//            System.out.println("MainFragment / _onCreateLoader / id = TITLE_LOADER");
//        else
//            System.out.println("MainFragment / _onCreateLoader / id = "+ id);

        // init loaded rows count
        rowsLoadedCount = 0;

        // init playlists
        mPlayLists = new ArrayList<>();

        // list for Show row number and link number
        rowInfoList = new ArrayList<>();


        if (id == CATEGORY_LOADER) {
            return new CursorLoader(
                    Objects.requireNonNull(getContext()),
                    PhotoContract.CategoryEntry.CONTENT_URI, // Table to query
                    // not show duplicated category name
                    new String[]{"DISTINCT " + PhotoContract.CategoryEntry.COLUMN_CATEGORY_NAME,
                            PhotoContract.CategoryEntry.COLUMN_VIDEO_TABLE_ID},
                    // show duplicated category name
//                    new String[]{VideoContract.CategoryEntry.COLUMN_CATEGORY_NAME},
                    // Only categories
                    null, // No selection clause
                    null, // No selection arguments
                    null  // Default sort order
            );

        }
        else if (id == TITLE_LOADER) {
            return new CursorLoader(
                    getContext(),
                    PhotoContract.VideoEntry.CONTENT_URI, // Table to query
                    new String[]{"DISTINCT " + PhotoContract.VideoEntry.COLUMN_ROW_TITLE},
                    // Only categories
                    null, // No selection clause
                    null, // No selection arguments
                    null  // Default sort order
            );

        } else {
            // Assume it is for a video.
            String title = args.getString(PhotoContract.VideoEntry.COLUMN_ROW_TITLE);
//            System.out.println("MainFragment / _onCreateLoader / title = "+ title);
            // This just creates a CursorLoader that gets all videos.
            return new CursorLoader(
                    getContext(),
                    PhotoContract.VideoEntry.CONTENT_URI, // Table to query
                    null, // Projection to return - null means return all fields
                    PhotoContract.VideoEntry.COLUMN_ROW_TITLE + " = ?", // Selection clause
                    new String[]{title},  // Select based on the rowTitle id.
                    null // Default sort order
            );
        }
    }

    private List<List> mPlayLists;

    int row_id;


    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
//        System.out.println("MainFragment / _onLoadFinished /  start rowsLoadedCount = " + rowsLoadedCount);
//        System.out.println("MainFragment / _onLoadFinished /  mVideoCursorAdapters.size() = " + mVideoCursorAdapters.size());

        // return when load is OK
        if( (rowsLoadedCount!=0 ) && (rowsLoadedCount >= mVideoCursorAdapters.size()) ) {
//            System.out.println("MainFragment / _onLoadFinished / return");//??? not needed this?
            return;
        }

        // cursor data is not null
        if (data != null && data.moveToFirst()) {
            final int loaderId = loader.getId();

//            if (loaderId == CATEGORY_LOADER)
//	            System.out.println("MainFragment / _onLoadFinished / loaderId = CATEGORY_LOADER");
//            else if(loaderId == TITLE_LOADER)
//                System.out.println("MainFragment / _onLoadFinished / loaderId = TITLE_LOADER");
//            else
//                System.out.println("MainFragment / _onLoadFinished / loaderId (video) = " + loaderId);

            if (loaderId == CATEGORY_LOADER) {
                mCategoryNames = new ArrayList<>();
                // Iterate through each category entry and add it to the ArrayAdapter.
                while (!data.isAfterLast()) {
                    int categoryIndex = data.getColumnIndex(PhotoContract.CategoryEntry.COLUMN_CATEGORY_NAME);
                    String category_name = data.getString(categoryIndex);
//                    System.out.println("MainFragment / _onLoadFinished / category_name = " + category_name);
                    mCategoryNames.add(category_name);

                    // check only
//                    int video_table_id_index = data.getColumnIndex(VideoContract.CategoryEntry.COLUMN_VIDEO_TABLE_ID);
//                    int video_table_id = data.getInt(video_table_id_index);
//                    System.out.println("MainFragment / _onLoadFinished / video_table_id = " + video_table_id);

                    data.moveToNext();
                }

                //start loading video
                mLoaderManager.initLoader(TITLE_LOADER, null, this);

            } else if (loaderId == TITLE_LOADER) {

                // create category list row
                createListRow_category();

                // create video list rows
                row_id = createListRows_video(data);

                // create option list row
                createListRow_option(row_id);

                // init row position and focus item
                setSelectedPosition(0);
                int pos = DbData.getFocusItemPosition_categoryRow(getContext());
                setSelectedPosition(0, true, new ListRowPresenter.SelectItemViewHolderTask(pos));

                startEntranceTransition(); //Move startEntranceTransition to after all

                if(!Pref.getPref_db_is_created(act)) {
                    // show toast
                    Toast.makeText(act, getString(R.string.db_is_updated), Toast.LENGTH_SHORT).show();

                    // update db_is_updated to be true
                    Pref.setPref_db_is_created(act,true);
                }

                /*
                 *  end of loading category
                 */
//                System.out.println("MainFragment / _onLoadFinished / -----------------------------------------");
//                System.out.println("MainFragment / _onLoadFinished / end of onLoadFinished category");
//                System.out.println("MainFragment / _onLoadFinished / -----------------------------------------");

            } else {
                // The CursorAdapter(mVideoCursorAdapters)
                // contains a Cursor pointing to all videos.
                if((mVideoCursorAdapters!= null) &&
                   (mVideoCursorAdapters.get(loaderId)!= null))
                    mVideoCursorAdapters.get(loaderId).changeCursor(data);

                // one row added
                rowsLoadedCount++;
//                System.out.println("MainFragment / _onLoadFinished / rowsLoadedCount = "+ rowsLoadedCount);

                /**
                 *  end of loading video
                 * */
                if(rowsLoadedCount == mVideoCursorAdapters.size() )
                {
                    setPlaylistsCount(rowsLoadedCount);
                    isOkToChangeCategory = true;

//                    System.out.println("MainFragment / _onLoadFinished / -------------------------------------");
//                    System.out.println("MainFragment / _onLoadFinished / end of onLoadFinished video");
//                    System.out.println("MainFragment / _onLoadFinished / -------------------------------------");

                    // create row info list
                    DbHelper mOpenHelper = new DbHelper(act);
                    mOpenHelper.setWriteAheadLoggingEnabled(false);
                    SQLiteDatabase sqlDb = mOpenHelper.getReadableDatabase();
                    String table = PhotoContract.VideoEntry.TABLE_NAME.concat(String.valueOf(Pref.getPref_video_table_id(act)));

                    Cursor cursor = sqlDb.query(
                            table,
                            null,//projection,
                            null,//selection,
                            null,//selectionArgs,
                            null,
                            null,
                            null//sortOrder
                    );

                    int columnIndex_ID = cursor.getColumnIndex(PhotoContract.VideoEntry._ID);
                    int columnIndex_row_title = cursor.getColumnIndex(PhotoContract.VideoEntry.COLUMN_ROW_TITLE);
                    int videosCount = cursor.getCount();
                    String same_row_title = null;
                    int size_row_links = 0;
                    int first_videoId = 0;
                    int last_videoId = 0;
                    List<Integer> play_list = null;

                    for(int pos=0;pos<videosCount;pos++) {

                        cursor.moveToPosition((int) pos);
                        int id = cursor.getInt(columnIndex_ID);
                        int end_id = 0;
                        String row_title = cursor.getString(columnIndex_row_title);

                        if(pos == videosCount-1)
                            end_id = id;

                        // new same row_title
                        if(!row_title.equalsIgnoreCase(same_row_title)){

                            // grouping
                            if(size_row_links>1){
                                mPlayLists.add(play_list);
                                rowInfoList.add(new RowInfo(first_videoId,last_videoId,size_row_links,play_list));
                            }

                            // new row start
                            size_row_links = 1;
                            first_videoId = id;
                            same_row_title = row_title;
                            play_list = new ArrayList<>();
                            play_list.add(id);
                        } else if (row_title.equalsIgnoreCase(same_row_title)){
                            last_videoId = id;
                            play_list.add(id);
                            size_row_links++;

                            // end of table
                            if(last_videoId == end_id) {
                                mPlayLists.add(play_list);
                                rowInfoList.add(new RowInfo(first_videoId, last_videoId, size_row_links, play_list));
                            }
                        }
                    }
                    cursor.close();
                    sqlDb.close();

                    // check row info list
//                    for(int i=0;i<rowInfoList.size();i++){
//                        System.out.println("( row  = " + i);
//                        List<Integer> list = rowInfoList.get(i).list;
//                        System.out.println("    size  = " + list.size());
//                        System.out.print("    from " + rowInfoList.get(i).start_id);
//                        System.out.println(" to " + rowInfoList.get(i).end_id +")");
//                    }

                }
            }
        } else { // cursor data is null after App installation
            /***
             *  call fetch data to load or update data base
             */

            // Start fetching the photo data
            if ((loader.getId() == CATEGORY_LOADER) && (mCategoryNames == null)) {
                System.out.println("MainFragment / onLoadFinished / start Fetch local data =================================");

                if(!Pref.getPref_db_is_created(act)) {
                    // data base is not created yet, check READ permission for the first time
                    // check permission first time, request necessary permission
                    if (!Utils.isGranted_permission_READ_EXTERNAL_STORAGE(act)) {
                        // request permission dialog
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M/*API23*/) {
                            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                                    PERMISSIONS_REQUEST_STORAGE);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        System.out.println("MainFragment / _onLoaderReset");

        int loaderId = loader.getId();
        if (loaderId != TITLE_LOADER) {
            mVideoCursorAdapters.get(loaderId).changeCursor(null);
        } else {
            mTitleRowAdapter.clear();
            mVideoCursorAdapters.clear();

            rowsLoadedCount = 0;
            mLoaderManager.restartLoader(CATEGORY_LOADER, null, this);
        }
    }

    //
    // create Category presenter
    //
    void createListRow_category(){ //todo 1st
        // set focus category
        int focusPos = DbData.getFocusItemPosition_categoryRow(getContext());
        CategoryListRowPresenter cate_listRowPresenter = new CategoryListRowPresenter(act,focusPos);
        cate_listRowPresenter.setRowHeight(400);

        // Map title results from the database to ListRow objects.
        // This Adapter is used to render the MainFragment sidebar labels.
        mTitleRowAdapter = new ArrayObjectAdapter(cate_listRowPresenter);
        setAdapter(mTitleRowAdapter);

        // category UI
        // Create a row for category selections at top
        String cate_name = Pref.getPref_category_name(act);
        String curr_cate_message;

        // initial category name
        if(cate_name.equalsIgnoreCase("no category name")){
            // get first available category name
            cate_name = mCategoryNames.get(INIT_CATEGORY_NUMBER - 1);
            Pref.setPref_category_name(getActivity(),cate_name);
        }

        // current category message
        curr_cate_message = act.getResources().
                getString(R.string.current_category_title).
                concat(" : ").
                concat(cate_name);

        // category header
        HeaderItem cate_gridHeader = new HeaderItem(curr_cate_message);

        // Category item presenter
        CategoryGridItemPresenter cate_gridItemPresenter= new CategoryGridItemPresenter(this,mCategoryNames);
        ArrayObjectAdapter cate_gridRowAdapter = new ArrayObjectAdapter(cate_gridItemPresenter);

        // show category name
        for(int i=1;i<= mCategoryNames.size();i++)
            cate_gridRowAdapter.add(mCategoryNames.get(i-1));

        // category list row
        ListRow cate_listRow = new ListRow(cate_gridHeader, cate_gridRowAdapter);

        // add category row
        mTitleRowAdapter.add(cate_listRow);
    }

    //
    // create Video presenter
    //
    int createListRows_video(Cursor data){
        // row id count start
        int row_id = 0;
//                listRowCategory.setId(row_id);

        // clear for not adding duplicate rows
        if(rowsLoadedCount != mVideoCursorAdapters.size())
        {
            //System.out.println("MainFragment / _onLoadFinished /  mTitleRowAdapter.clear()");
            // Every time we have to re-get the category loader, we must re-create the sidebar.
            mTitleRowAdapter.clear();
        }

        // Iterate through each category entry and add it to the ArrayAdapter.
        while (!data.isAfterLast()) {
            int titleIndex = data.getColumnIndex(PhotoContract.VideoEntry.COLUMN_ROW_TITLE);
            String title = data.getString(titleIndex);
//            System.out.println("MainFragment / _onLoadFinished / title = " + title);

            // Create header for this category.
            HeaderItem header = new HeaderItem(title);
//            System.out.println("MainFragment / _onLoadFinished / header.getName() = " + header.getName());

            // if (getHeadersSupportFragment() != null){
                // on selected
//                getHeadersSupportFragment().setOnHeaderViewSelectedListener(new HeadersSupportFragment.OnHeaderViewSelectedListener() {
//                    @Override
//                    public void onHeaderSelected(RowHeaderPresenter.ViewHolder viewHolder, Row row) {
//                        System.out.println("MainFragment / _onLoadFinished / setOnHeaderViewSelectedListener /" +
//                                "  = " + row.getId() + " / "
//                                + row.getHeaderItem().getName());
//                    }
//                });

                // on clicked
//                getHeadersSupportFragment().setOnHeaderClickedListener(new HeadersSupportFragment.OnHeaderClickedListener() {
//                    @Override
//                    public void onHeaderClicked(RowHeaderPresenter.ViewHolder viewHolder, Row row) {
//                        System.out.println("MainFragment / _onLoadFinished / setOnHeaderClickedListener /" +
//                                " row ID = " + row.getId() + " / header name = "
//                                + row.getHeaderItem().getName());
//                    }
//                });
//            }

            int videoLoaderId = title.hashCode(); // Create unique int from title.
            CursorObjectAdapter existingAdapter = mVideoCursorAdapters.get(videoLoaderId);
            row_id++;
            if (existingAdapter == null) {

                // Map video results from the database to Video objects.
                CursorObjectAdapter videoCursorAdapter = new CursorObjectAdapter(new CardPresenter(act,row_id));
                videoCursorAdapter.setMapper(new VideoCursorMapper());

                // Base of data source: videoCursorAdapter
                mVideoCursorAdapters.put(videoLoaderId, videoCursorAdapter);

                ListRow row = new ListRow(header, videoCursorAdapter);
                mTitleRowAdapter.add(row);
                row.setId(row_id);
	            // System.out.println("MainFragment / _onLoadFinished / existingAdapter is null  / will initLoader / videoLoaderId = " + videoLoaderId);

                // Start loading the videos from the database for a particular category.
                Bundle args = new Bundle();
                args.putString(PhotoContract.VideoEntry.COLUMN_ROW_TITLE, title);

                // init loader for video items
                mLoaderManager.initLoader(videoLoaderId, args, this);
            } else {
                // System.out.println("MainFragment / _onLoadFinished / existingAdapter is not null ");
                ListRow row = new ListRow(header, existingAdapter);
                row.setId(row_id);
                mTitleRowAdapter.add(row);
            }

            //System.out.println("MainFragment / _onLoadFinished / loaderId == TITLE_LOADER / rowsLoadedCount = " + rowsLoadedCount);
            data.moveToNext();
        }
        return row_id;
    }

    //
    // create Option presenter
    //
    void createListRow_option(int _row_id){

        row_id = _row_id;

        // Create a row for this special case with more samples.
        HeaderItem gridHeader = new HeaderItem(getString(R.string.options));
        GridItemPresenter gridPresenter = new GridItemPresenter(this);
        ArrayObjectAdapter gridRowAdapter = new ArrayObjectAdapter(gridPresenter);
        gridRowAdapter.add(getString(R.string.select_category));
//        gridRowAdapter.add(getString(R.string.category_grid_view_title));
        gridRowAdapter.add(getString(R.string.personal_settings));
        ListRow row = new ListRow(gridHeader, gridRowAdapter);

        row_id++;
        row.setId(row_id);
        mTitleRowAdapter.add(row);
    }

    /**
     * runnable for counting down
     */
    private Runnable runCountDown = new Runnable() {
        public void run() {
            // show count down
            TextView messageView = (TextView) alertDlg.findViewById(android.R.id.message);
            count--;
            countStr = act.getString(R.string.play_countdown)+
                                " " + count + " " +
                              act.getString(R.string.play_time_unit);
            countStr = countStr.replaceFirst("[0-9]",String.valueOf(count));
            messageView.setText( act.getString(R.string.play_4_spaces)+ nextLinkTitle +"\n\n" +countStr);

            if(count>0)
                handler.postDelayed(runCountDown,1000);
            else
            {
                // launch next intent
                alertDlg.dismiss();
                cancelPhotoHandler();
                launchPhotoIntent();
            }
        }
    };

    int delay100ms = 100;
    /**
     *  launch next photo intent
     */
    private void launchPhotoIntent()
    {
//        if(MainFragment.currLinkId >= MainFragment.getCurrLinksLength())
        //refer: https://developer.android.com/reference/android/view/KeyEvent.html#KEYCODE_DPAD_DOWN_RIGHT

        // check if at the end of row
        if(isRowEnd())
        {
            // from test result current capability is shift left 15 steps only
            DPadAsyncTask task = new DPadAsyncTask(backSteps);
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
        else
        {
            // delay
            try {
                Thread.sleep(delay100ms *10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // shift right 1 step
            BaseInputConnection mInputConnection = new BaseInputConnection(act.findViewById(R.id.main_frame), true);
            mInputConnection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT));
            mInputConnection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_RIGHT));

            // delay
            try {
                Thread.sleep(delay100ms * 10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // set new play Id
            setPlayId(getNextId_auto());

            // method 1: by intent
            startAutoPhotoIntentForResult(getPhotoPosition());

            // method 2 : by UI
//            mInputConnection = new BaseInputConnection(act.findViewById(R.id.main_frame), true);
//            mInputConnection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_CENTER ));
//            mInputConnection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_CENTER));

        }
    }

    // start photo intent for result
    private void startAutoPhotoIntentForResult(int position){
        Intent intent = new Intent(act, AutoPhotoAct.class);
        intent.putExtra("PHOTO_POSITION", position);
        startActivityForResult(intent,PHOTO_INTENT);
    }


    // start photo intent
    private void startAutoPhotoIntent(int position){
        Intent intent = new Intent(act, AutoPhotoAct.class);
        intent.putExtra("PHOTO_POSITION", position);
        startActivity(intent);
    }

    // start photo fragment
    private void startManualPhotoFragment(String path)
    {
        ManualPhotoFragment fragment = new ManualPhotoFragment();
        final Bundle args = new Bundle();
        args.putInt("PHOTO_POSITION", getPhotoPosition());
        fragment.setArguments(args);
        FragmentTransaction transaction = act.getSupportFragmentManager().beginTransaction();
        transaction.setCustomAnimations(R.anim.fragment_slide_in_left, R.anim.fragment_slide_out_left, R.anim.fragment_slide_in_right, R.anim.fragment_slide_out_right);
        transaction.replace(R.id.main_frame, fragment,"photo").addToBackStack("photo_stack").commit();
    }

    private void cancelPhotoHandler()
    {
        System.out.println("MainFragment / _cancelPhotoHandler");
        if(handler != null) {
            handler.removeCallbacks(runCountDown);
            handler = null;
        }
    }

    private class DPadAsyncTask extends AsyncTask<Void, Integer, Void> {
        int dPadSteps;
        DPadAsyncTask(int dPadSteps)
        {
            this.dPadSteps = dPadSteps;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected Void doInBackground(Void... params)
        {
            BaseInputConnection mInputConnection = new BaseInputConnection(act.findViewById(R.id.main_frame), true);

            // point to first item of current row
            for(int i=0;i<dPadSteps;i++)
            {
                System.out.println("MainFragment / DPadAsyncTask / back i = " + i);
                mInputConnection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_LEFT));
                mInputConnection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_LEFT));
                try {
                    Thread.sleep(delay100ms*2);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            // for auto play by category only
            if(Pref.isCyclicByCategory(act)) {
                // add delay to make sure key event works
                try {
                    Thread.sleep(delay100ms * 2);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                // TBD: change to other row, problem: item selected is not reset to 1st position
                // point to first row if meets the end of last row
                List lastRowList = mPlayLists.get(mPlayLists.size()-1);
                if (getPlayId() == (int)lastRowList.get(lastRowList.size()-1)) {
                    for (int i = (mPlayLists.size() - 1); i >= 1; i--) {
                        // shift up
                        mInputConnection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_UP));
                        mInputConnection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_UP));
                        try {
                            Thread.sleep(delay100ms * 2);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    // point to next row
                    BaseInputConnection connection = new BaseInputConnection(act.findViewById(R.id.main_frame), true);
                    connection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_DOWN));
                    connection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_DOWN));
                }

                // add delay for viewer
                try {
                    Thread.sleep(delay100ms * 10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);

            // set new play Id after Shift keys
            setPlayId(getNextId_auto());

            startAutoPhotoIntentForResult(getPhotoPosition());
        }
    }

    private int backSteps;

    // check if Row End
    private boolean isRowEnd()
    {
        boolean isEnd = false;
        System.out.println("isRowEnd / getNextId_auto() = " + getNextId_auto());
        backSteps = 0;

        // for auto play by category only
        if(Pref.isCyclicByCategory(act)) {
            int rowsCount = mPlayLists.size();
            for (int row = 0; row < rowsCount; row++) {
                List<Integer> playlist = mPlayLists.get(row);
                int lastIdOfRow = playlist.get(playlist.size()-1);
                if (lastIdOfRow == getPlayId() ) {
                    isEnd = true;
                    // back steps
                    backSteps = mPlayLists.get(row).size() - 1;
                    break;
                }
            }
        }

        // for auto play by list only
        if(Pref.isCyclicByList(act)) {
            if (getNextId_auto() == currentRow1stId) {
                backSteps = currentRowSize - 1;
                isEnd = true;
            }
        }

        System.out.println("isRowEnd / isEnd = " + isEnd);
        System.out.println("isRowEnd / backSteps = " + backSteps);

        return isEnd;
    }

    private void updateBackground(String uri) {
        int width = mMetrics.widthPixels;
        int height = mMetrics.heightPixels;

        RequestOptions options = new RequestOptions()
                .centerCrop()
                .error(mDefaultBackground);

        Glide.with(act)
                .asBitmap()
                .load(uri)
                .apply(options)
                .into(new CustomTarget<Bitmap>(width, height) {
                    @Override
                    public void onResourceReady(
                            Bitmap resource,
                            Transition<? super Bitmap> transition) {
                        mBackgroundManager.setBitmap(resource);
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {

                    }
                });
    }

    private void startBackgroundTimer() {
        mHandler.removeCallbacks(mBackgroundTask);
        mHandler.postDelayed(mBackgroundTask, BACKGROUND_UPDATE_DELAY);
    }

//    private void updateRecommendations() {
//        Intent recommendationIntent = new Intent(act, UpdateRecommendationsService.class);
//        act.startService(recommendationIntent);
//    }

    // get default URL
    private String getDefaultUrl(int init_number)
    {
        // in res/values
//        String name = "db_source_id_".concat(String.valueOf(init_number));
//        int res_id = Objects.requireNonNull(act)
//                .getResources().getIdentifier(name,"string",act.getPackageName());
//        return "https://drive.google.com/uc?export=download&id=" +  getString(res_id);

        // in assets
        List<Pair<String, String>> src_links = Source_links.getFileIdList(Objects.requireNonNull(act));
        int index = init_number -1; // starts from 1
//        // note: AND sign expression
//        //  in XML: &amp;
//        //  in Java: &
        return "https://drive.google.com/uc?export=download&id=" + src_links.get(index).getSecond();
    }

    private class UpdateBackgroundTask implements Runnable {

        @Override
        public void run() {
            if (mBackgroundURI != null) {
                updateBackground(mBackgroundURI.toString());
            }
        }
    }

    // play id
    private int play_id;
    private void setPlayId(int id)
    {
        play_id = id;
    }

    private int getPlayId()
    {
        return play_id;
    }

    // set next id for Auto
    private int next_id;
    private void setNextId_auto(int id) {
        next_id = id;
    }

    // get next ID for Auto
    private int getNextId_auto() {
        return next_id;
    }

    // get photo path
    private String getPhotoPath() {
        int focusCatNum = Pref.getPref_video_table_id(act);
        String table = PhotoContract.VideoEntry.TABLE_NAME.concat(String.valueOf(focusCatNum));
        String columnName = PhotoContract.VideoEntry.COLUMN_THUMB_URL;
        int pos = DbData.getCursorPositionById(getContext(),getPlayId());
        return DbData.getDB_link_data(getContext(),table,columnName,pos);
    }

    // get photo position
    private int getPhotoPosition() {
        int pos = DbData.getCursorPositionById(getContext(),getPlayId());
        return pos;
    }

    // get next photo title
    private String getNextPhotoTitle() {
        int focusCatNum = Pref.getPref_video_table_id(act);
        String table = PhotoContract.VideoEntry.TABLE_NAME.concat(String.valueOf(focusCatNum));
        String columnName = PhotoContract.VideoEntry.COLUMN_LINK_TITLE;
        int pos = DbData.getCursorPositionById(getContext(),getNextId_auto());
        return DbData.getDB_link_data(getContext(),table,columnName,pos);
    }

    // get next cursor position ID for Auto play
    int getNextCursorPositionId_auto(int currentPlayId){
        int focusCatNum = Pref.getPref_video_table_id(act);
        String table = PhotoContract.VideoEntry.TABLE_NAME.concat(String.valueOf(focusCatNum));

        int cursorPos = 0;
        DbHelper mOpenHelper = new DbHelper(act);
        mOpenHelper.setWriteAheadLoggingEnabled(false);
        SQLiteDatabase sqlDb = mOpenHelper.getReadableDatabase();
        Cursor cursor = sqlDb.query(
                table,
                null,//projection,
                null,//selection,
                null,//selectionArgs,
                null,
                null,
                null//sortOrder
        );

        int index_id = cursor.getColumnIndex("_id");
        for(int position=0;position<cursor.getCount();position++){
            cursor.moveToPosition((int) position);
            if(currentPlayId == cursor.getInt(index_id)) {
                cursorPos = position;
                break;
            }
        }

        // check row position
        for (int i = 0; i < mPlayLists.size(); i++) {
            if (currentPlayId == (int) mPlayLists.get(i).get(0)) {
                currentRowPos = i;
                break;
            }
        }

        // video ID starts with 1
        currentRow1stId = (int) mPlayLists.get(currentRowPos).get(0);
        currentRowSize = mPlayLists.get(currentRowPos).size();
        currentRowLastId = (int) mPlayLists.get(currentRowPos).get(currentRowSize-1);//currentRow1stId + currentRowSize - 1; //todo last item error

        int nextId = 0;
        // at last video item of category
        try{
            cursor.moveToPosition(cursorPos+1);
            cursor.getInt(index_id);
        } catch(Exception e){
            // set next ID
            if(Pref.isCyclicByList(act))
                nextId = currentRow1stId;
            else if (Pref.isCyclicByCategory(act)) {
                cursor.moveToPosition(0);
                nextId = cursor.getInt(index_id);
            }

            cursor.close();
            sqlDb.close();
            return nextId;
        }

        // move cursor to next position
        cursor.moveToPosition(cursorPos+1);

        // at row end
        if(cursor.getInt(index_id)  > currentRowLastId  ) {
            // set next ID
            if(Pref.isCyclicByList(act))
                nextId = currentRow1stId;
            else if(Pref.isCyclicByCategory(act)) {
                nextId = cursor.getInt(index_id);
            }
        } else {
            cursor.moveToPosition((int) cursorPos + 1);
            nextId = cursor.getInt(index_id);
        }

        cursor.close();
        sqlDb.close();
        return  nextId;
    }

    // get duplicated times of same category name
    // i.e.
    // 1. JSON file is the same
    // 2. category names in DB are different
    public static int getCategoryNameDuplicatedTimes(String categoryName){
        int size = mCategoryNames.size();
        int duplicatedTimes = 0;

        for(int i=0;i<size;i++) {
            if (mCategoryNames.get(i).contains(categoryName))
                duplicatedTimes++;
        }
        return duplicatedTimes;
    }

    static int rowsCount;
    // set playlists count
    public void setPlaylistsCount(int count){
        rowsCount = count;
    }

    // get playlists count
    public static int getPlaylistsCount(){
        return rowsCount;
    }

    // differentiate category row with other rows
    boolean isCategoryRow(String catName){
        for(int i=0;i<mCategoryNames.size();i++) {
            if(catName.equalsIgnoreCase(mCategoryNames.get(i)))
                return true;
        }
        return false;
    }

    // open video item
    void openVideoItem(Object item){
        Video video = (Video) item;
//        System.out.println("MainFragment / _openVideoItem / item = "+ item );

        // for auto play by list only
        if(!Pref.isCyclicByCategory(act)) {
            // check row position
            for (int i = 0; i < mPlayLists.size(); i++) {
                if (video.id >= (int) mPlayLists.get(i).get(0))
                    currentRowPos = i;
            }
        }

        // video ID starts with 1
        currentRow1stId = (int) mPlayLists.get(currentRowPos).get(0);
        currentRowSize = mPlayLists.get(currentRowPos).size();
        currentRowLastId = (int) mPlayLists.get(currentRowPos).get(currentRowSize-1);//currentRow1stId + currentRowSize - 1; //todo last item error

        // auto play
        if (Pref.isAutoPlay(act)){
            setPlayId((int) ((Video) (item)).id);

            if(Define.DEFAULT_PLAY_NEXT == Define.by_onActivityResult)
                startAutoPhotoIntentForResult(getPhotoPosition());
            else if(Define.DEFAULT_PLAY_NEXT == Define.by_runnable)
                startAutoPhotoIntent(getPhotoPosition());

        } else {
            // manual play
            act.runOnUiThread(new Runnable() {
                public void run() {
                    // for open directly
                    setPlayId((int) ((Video) (item)).id);
                    startManualPhotoFragment(getPhotoPath());
                }
            });
        }
    }

    // row information class
    public static class RowInfo {
        public long start_id;
        public long end_id;
        public List<Integer> list;
        public int row_length;
        RowInfo(long start_id, long end_id, int row_length,List<Integer> list) {
            this.start_id = start_id;
            this.end_id = end_id;
            this.row_length = row_length;
            this.list = list;
        }
    }

    // get row info by video id
    public static int getRowInfoByVideoId(long videoId){
        // set position info text view : get link id & current row length
        // Sorting pair : original row length pair below could be not ordered

        RowInfo rowLen1, rowLen2;
        List<RowInfo> rowLenList = rowInfoList;

        int rows_count = rowLenList.size();
        int current_row_len = 0;

        long start = 0,start1,start2;
        long end = 0,end1,end2;
        for(int i=0;i<rows_count;i++) {
            rowLen1 = rowLenList.get(i);
            start1 = rowLen1.start_id;
            end1 = rowLen1.end_id;

            for(int j=0;j<rows_count;j++) {

                rowLen2 = rowLenList.get(j);
                start2 = rowLen2.start_id;
                end2 = rowLen2.end_id;

                if(start2 < start1) {
                    start = start2;
                    end = end2;
                    current_row_len = rowLen2.row_length;
                } else {
                    start = start1;
                    end = end1;
                    current_row_len = rowLen1.row_length;
                }
            }

            if ((start <= videoId) && (videoId <= end)) {
                break;
            }
        }
        return current_row_len;
    }

    // get link position in row by video ID
    public static int getLinkPositionInRowByVideoId(long video_id){
        int rowsCount = rowInfoList.size();
        int itemRowNumber = 0;
        for(int i=0;i<rowsCount;i++){
            RowInfo rowInfo = rowInfoList.get(i);
            if( (video_id >= rowInfo.start_id) &&
                (video_id <= rowInfo.end_id)      ){
                List list = rowInfo.list;

                for(int pos=0;pos<list.size();pos++){
                    if(video_id == (int)list.get(pos)) {
                        itemRowNumber = pos + 1;
                        break;
                    }
                }
                break;
            }
        }
        return itemRowNumber;
    }

//    final static int ACTION_MANAGE_ALL_FILES_ACCESS_REQUEST_CODE = 501;
    // check permission
//    void checkPermission(){
//        System.out.println("MainFragment / _checkPermission / Build.VERSION.SDK_INT = " + Build.VERSION.SDK_INT);
//
//        // <= API 29
//        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q /*API 29*/) {
//            // check permission first time, request necessary permission
//            if (!Utils.isGranted_permission_READ_EXTERNAL_STORAGE(getActivity())) {
//                // request permission dialog
//                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
//                        Utils.PERMISSIONS_REQUEST_STORAGE);
//            } else {
//                // case: renew default data
//                if (docDir == null) {
//                    // permission is granted
//                    LocalData.createCategoryDB2(getActivity());
//                }
//            }
//        }
//
//        // >= API 30
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R /*API 30*/) {
//            if(!Environment.isExternalStorageManager())
//            {
//                // intent to request Manage All permission
//                Intent intent = new Intent();
//                intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
//                startActivityForResult(intent, ACTION_MANAGE_ALL_FILES_ACCESS_REQUEST_CODE);
//            } else {
//
//                // permission is granted
//                LocalData.createCategoryDB2(getActivity());
//            }
//        }
//
//    }

    // callback of granted permission
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        System.out.println("MainFragment / _onRequestPermissionsResult / grantResults.length =" + grantResults.length);
        if ( (grantResults.length > 0) &&
             (grantResults[0] == PackageManager.PERMISSION_GRANTED) ){
            if (requestCode == PERMISSIONS_REQUEST_STORAGE) {
                // permission is granted
                Intent intent = new Intent(getActivity(), ScanLocalAct.class);
                startActivity(intent);
            }
        }
        else {
            //normally, will go to _resume if not finish
        }
    }
}