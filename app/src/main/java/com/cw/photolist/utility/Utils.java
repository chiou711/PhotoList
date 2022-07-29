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

package com.cw.photolist.utility;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.widget.Button;
import android.widget.Toast;

import com.cw.photolist.R;
import com.cw.photolist.data.DbHelper;
import com.cw.photolist.data.PhotoContract;
import com.cw.photolist.data.PhotoProvider;
import com.cw.photolist.define.Define;
import com.cw.photolist.ui.MainActivity;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static com.cw.photolist.ui.MainFragment.mCategoryNames;

/**
 * A collection of utility methods, all static.
 */
public class Utils {


    /*
     * Making sure public utility methods remain static
     */
    private Utils() {
    }

    public static String getYoutubeId(String url) {

        String videoId = "";

        if (url != null && url.trim().length() > 0 && url.startsWith("http")) {
            String expression = "^.*((youtu.be\\/)|(v\\/)|(\\/u\\/w\\/)|(embed\\/)|(watch\\?))\\??(v=)?([^#\\&\\?]*).*";
            CharSequence input = url;
            Pattern pattern = Pattern.compile(expression, Pattern.CASE_INSENSITIVE);//??? some Urls are NG
            Matcher matcher = pattern.matcher(input);
            if (matcher.matches()) {
                String groupIndex1 = matcher.group(8);
                if (groupIndex1 != null && groupIndex1.length() == 11)
                    videoId = groupIndex1;
            }
        }
        return videoId;
    }

    // get video tables count
    public static int getVideoTablesCount(Context context){
        // get video tables count
        DbHelper mOpenHelper = new DbHelper(context);
        mOpenHelper.setWriteAheadLoggingEnabled(false);
        SQLiteDatabase sqlDb = mOpenHelper.getReadableDatabase();

        String SQL_GET_ALL_TABLES = "SELECT * FROM sqlite_master WHERE name like 'video%'";
        Cursor cursor = sqlDb.rawQuery(SQL_GET_ALL_TABLES, null);
        int countVideoTables = cursor.getCount();
        cursor.close();
        sqlDb.close();
        return countVideoTables;
    }

    // get App default storage directory name
    static public String getStorageDirName(Context context)
    {
        Resources currentResources = context.getResources();
        Configuration conf = new Configuration(currentResources.getConfiguration());
        conf.locale = Locale.ENGLISH; // apply English to avoid reading directory error
        Resources newResources = new Resources(context.getAssets(),
                currentResources.getDisplayMetrics(),
                conf);
        String dirName = newResources.getString(R.string.dir_name);

        // restore locale
        new Resources(context.getAssets(),
                currentResources.getDisplayMetrics(),
                currentResources.getConfiguration());

        System.out.println("Utils / _getStorageDirName / dirName = " + dirName);
        return dirName;
    }

    // is Empty string
    public static boolean isEmptyString(String str)
    {
        boolean empty = true;
        if( str != null )
        {
            if(str.length() > 0 )
                empty = false;
        }
        return empty;
    }

    // Get video_table_id by category name
    public static int getVideoTableId_byCategoryName(Context act,String categoryName){
        int videoTableId = 0;

//        System.out.println("Utils / _getVideoTableId_byCategoryName /  categoryName =　" + categoryName);

        // initial video table ID
        if(categoryName.equalsIgnoreCase("no category name"))
            return Define.INIT_CATEGORY_NUMBER;

        DbHelper mOpenHelper = new DbHelper(act);
        mOpenHelper.setWriteAheadLoggingEnabled(false);

        SQLiteDatabase sqlDb;
        sqlDb = mOpenHelper.getReadableDatabase();

        Cursor cursor = sqlDb.query(
                "category",
                new String[]{"video_table_id"},
                "category_name="+"\""+ categoryName+"\"",
                null,
                null,
                null,
                null);

        cursor.moveToFirst();

        try {
            int columnIndex = cursor.getColumnIndex(PhotoContract.CategoryEntry.COLUMN_VIDEO_TABLE_ID);
            videoTableId = cursor.getInt(columnIndex);
        }catch (Exception e){
            e.printStackTrace();
            System.out.println(" cursor get int : error !");
        }

        cursor.close();
        sqlDb.close();
        mOpenHelper.close();

        return videoTableId;
    }

    // Delete category confirmation
    public static void confirmDeleteCategory(FragmentActivity act, List<String> mCategoryNames, String item){
        AlertDialog.Builder builder;
        builder = new AlertDialog.Builder(act);

        builder.setTitle(R.string.confirm_dialog_title)
                .setMessage( act.getResources().getString(R.string.delete_category_message) + " " + item )
                .setPositiveButton(act.getString(R.string.button_cancel), new DialogInterface.OnClickListener()
                {
                    // stop
                    @Override
                    public void onClick(DialogInterface dialog, int which1)
                    {
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(act.getString(R.string.button_ok), new DialogInterface.OnClickListener()
                {
                    // continue
                    @Override
                    public void onClick(DialogInterface dialog, int which1)
                    {
                        dialog.dismiss();
                        deleteSelectedCategory(act,mCategoryNames,(String)item);
                    }
                }).
                setOnCancelListener(new DialogInterface.OnCancelListener(){
                    // cancel
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        dialog.dismiss();
                    }
                } );
        AlertDialog alertDlg = builder.create();

        // set listener for selection
        alertDlg.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dlgInterface) {

                // focus
                Button negative = alertDlg.getButton(AlertDialog.BUTTON_NEGATIVE);
                negative.setFocusable(true);
                negative.setFocusableInTouchMode(true);
                negative.requestFocus();
            }
        });
        alertDlg.show();
    }

    // delete selected category
    static void deleteSelectedCategory(FragmentActivity act, List<String> mCategoryNames, String item){
        DbHelper mOpenHelper = new DbHelper(act);
        mOpenHelper.setWriteAheadLoggingEnabled(false);
        SQLiteDatabase sqlDb = mOpenHelper.getWritableDatabase();

        // get video table ID
        int videoTableId = Utils.getVideoTableId_byCategoryName(act.getApplicationContext(),(String)item);
        System.out.println("Utils / _deleteSelectedCategory / videoTableId = " + videoTableId);

        // Drop video table command
        final String SQL_DROP_VIDEO_TABLE = "DROP TABLE IF EXISTS " +
                PhotoContract.VideoEntry.TABLE_NAME.concat(String.valueOf(videoTableId));

        System.out.println(" SQL_DROP_VIDEO_TABLE = " + SQL_DROP_VIDEO_TABLE);

        // Execute drop command
        sqlDb.execSQL(SQL_DROP_VIDEO_TABLE);
        sqlDb.close();
        mOpenHelper.close();

        // delete current row in category table after drop its video table
        ContentResolver contentResolver = act.getApplicationContext().getContentResolver();
        contentResolver.delete(PhotoContract.CategoryEntry.CONTENT_URI,
                "category_name=" + "\""+(String)item+"\"" ,
                null);

        Intent returnIntent = new Intent();
        returnIntent.putExtra("KEY_DELETE", Pref.DB_DELETE);
        act.setResult( Activity.RESULT_OK, returnIntent);

        // show toast
        act.runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(act, act.getString(R.string.database_delete_item), Toast.LENGTH_SHORT).show();
            }
        });

        // update category names, in order get next available category name
        for(int i=0;i< mCategoryNames.size();i++){
            if(mCategoryNames.get(i).equalsIgnoreCase((String)item))
                mCategoryNames.remove(i);
        }

        // update focus with first category name
        Pref.setPref_category_name(act,mCategoryNames.get(0));

        // start new MainActivity
        Intent new_intent = new Intent(act, MainActivity.class);
        new_intent.addFlags(FLAG_ACTIVITY_CLEAR_TASK);
        new_intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
        act.startActivity(new_intent);
        act.finish();
    }

    // Delete playlist confirmation
    public static void confirmDeletePlaylist(FragmentActivity act, String item){

        AlertDialog.Builder builder;
        builder = new AlertDialog.Builder(act);

        builder.setTitle(R.string.confirm_dialog_title)
                .setMessage( act.getResources().getString(R.string.delete_playlist_message) + " " + item )
                .setPositiveButton(act.getString(R.string.button_cancel), new DialogInterface.OnClickListener()
                {
                    // stop
                    @Override
                    public void onClick(DialogInterface dialog, int which1)
                    {
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(act.getString(R.string.button_ok), new DialogInterface.OnClickListener()
                {
                    // continue
                    @Override
                    public void onClick(DialogInterface dialog, int which1)
                    {
                        dialog.dismiss();
                        deleteSelectedPlaylist(act,(String)item);
                    }
                }).
                setOnCancelListener(new DialogInterface.OnCancelListener(){
                    // cancel
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        dialog.dismiss();
                    }
                } );
        AlertDialog alertDlg = builder.create();

        // set listener for selection
        alertDlg.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dlgInterface) {

                // focus
                Button negative = alertDlg.getButton(AlertDialog.BUTTON_NEGATIVE);
                negative.setFocusable(true);
                negative.setFocusableInTouchMode(true);
                negative.requestFocus();
            }
        });
        alertDlg.show();
    }

    // delete selected playlist
    static void deleteSelectedPlaylist(FragmentActivity act, String item){

        System.out.println("Utils / _deleteSelectedPlaylist / item = " + item);
//        System.out.println("Utils / _deleteSelectedPlaylist / video table_id = " + video_table_id);

        // delete current item
        ContentResolver contentResolver = act.getApplicationContext().getContentResolver();
        PhotoProvider.tableId = String.valueOf(Pref.getPref_video_table_id(act));
        System.out.println("Utils / _deleteSelectedPlaylist / VideoProvider.tableId = " + PhotoProvider.tableId);
        contentResolver.delete(PhotoContract.VideoEntry.CONTENT_URI, "row_title=" + "\""+item+"\"",null);

        // check if playlist still exists
        Cursor cursor = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            // Call requires API level 26(8.0)
            cursor = contentResolver.query(PhotoContract.VideoEntry.CONTENT_URI, null,null,null);
        }
        int leftRows = cursor.getCount();
        cursor.close();

        if(leftRows == 0) {
            String currCategoryName = Pref.getPref_category_name(act);
            deleteSelectedCategory(act, mCategoryNames, currCategoryName);
        }
        else {
            // start new MainActivity
            Intent new_intent = new Intent(act, MainActivity.class);
            new_intent.addFlags(FLAG_ACTIVITY_CLEAR_TASK);
            new_intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
            act.startActivity(new_intent);
            act.finish();
        }
    }

    // Delete selected item confirmation
    public static void confirmDeleteSelectedItem(FragmentActivity act, long video_id){

        AlertDialog.Builder builder;
        builder = new AlertDialog.Builder(act);

        builder.setTitle(R.string.confirm_dialog_title)
                .setMessage( act.getResources().getString(R.string.delete_item_message) + " " + video_id )
                .setPositiveButton(act.getString(R.string.button_cancel), new DialogInterface.OnClickListener()
                {
                    // stop
                    @Override
                    public void onClick(DialogInterface dialog, int which1)
                    {
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(act.getString(R.string.button_ok), new DialogInterface.OnClickListener()
                {
                    // continue
                    @Override
                    public void onClick(DialogInterface dialog, int which1)
                    {
                        dialog.dismiss();
                        deleteSelectedItem(act,video_id);
                    }
                }).
                setOnCancelListener(new DialogInterface.OnCancelListener(){
                    // cancel
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        dialog.dismiss();
                    }
                } );
        AlertDialog alertDlg = builder.create();

        // set listener for selection
        alertDlg.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dlgInterface) {

                // focus
                Button negative = alertDlg.getButton(AlertDialog.BUTTON_NEGATIVE);
                negative.setFocusable(true);
                negative.setFocusableInTouchMode(true);
                negative.requestFocus();
            }
        });
        alertDlg.show();
    }

    // delete selected item
    public static void deleteSelectedItem(FragmentActivity act, long video_id){

        System.out.println("Utils / _deleteSelectedItem / id = " + video_id);
        ContentResolver contentResolver = act.getApplicationContext().getContentResolver();
        PhotoProvider.tableId = String.valueOf(Pref.getPref_video_table_id(act));
        contentResolver.delete(PhotoContract.VideoEntry.CONTENT_URI, "_id=" + video_id,null);

        Intent returnIntent = new Intent();
        returnIntent.putExtra("KEY_DELETE", Pref.DB_DELETE);
        act.setResult( Activity.RESULT_OK, returnIntent);

        // show toast
        act.runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(act, act.getString(R.string.database_delete_item), Toast.LENGTH_SHORT).show();
            }
        });

        act.finish();
    }

    public static final int PERMISSIONS_REQUEST_STORAGE = 12;
    // check if READ_EXTERNAL_STORAGE permission is granted
    public static boolean isGranted_permission_READ_EXTERNAL_STORAGE(Activity act){

        int permissionWriteExtStorage = ActivityCompat.checkSelfPermission(act,
                Manifest.permission.READ_EXTERNAL_STORAGE);

        if(permissionWriteExtStorage != PackageManager.PERMISSION_GRANTED )
            return false;
        else
            // granted
            return true;
    }

}
