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

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import com.cw.photolist.R;
import com.cw.photolist.data.DbHelper;
import com.cw.photolist.data.VideoContract;
import com.cw.photolist.data.VideoProvider;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * A collection of utility methods, all static.
 */
public class LocalData {
    // return array type
    public static int CATEGORY_DATA = 1;
    public static int PHOTO_DATA = 2;

    // for scanning local directory
    public static List<String> category_array;
    public static List<Photo> photo_array;

    // limit for create new folder
    static int PAGES_PER_FOLDER = 7;

    static Integer folders_count;
    static Integer pages_count = 0;
    static List<String> filePathArray = null;
    static List<String> fileNames = null;
    public static String currFilePath;
    static String docDir;

    // init
    public static void init(String _docDir){
        currFilePath = _docDir;
        docDir = _docDir;
        folders_count = 0;
        pages_count = 0;
        category_array = new ArrayList<>();
        photo_array = new ArrayList<>();
    }

    static String listTitle = null;

    // Scan all storage devices and save audio links to DB
    public static void scan_and_save(String currFilePath, boolean beSaved, int returnType){
        System.out.println("LocalData / _scan_and_save / currFilePath = " + currFilePath);

        List<String> list;
        list = getListInPath(currFilePath);

        if (list.size() > 0 ) {
            for (String file : list) {
                File fileDir = new File(currFilePath.concat("/").concat(file));
                System.out.println("==>  file = " + file);
                System.out.println("==>  fileDir = " + fileDir.getPath());

                boolean check =  !fileDir.getAbsolutePath().contains("..") ||
                        (fileDir.getAbsolutePath().contains("..") &&  (file.length()!=2) ) ;

                //Skip some directories which could cause playing hang-up issue
                if( /*!fileDir.getAbsolutePath().contains("Android/data") && */
                    !fileDir.getAbsolutePath().contains(".thumbnails") &&
                    !fileDir.getAbsolutePath().contains("Android/media") &&
                                check )
                {
                    if (fileDir.isDirectory()) {

                        // add page
                        int dirs_count = 0;
                        int dirsFilesCount = 0;

                        // get page name
                        String pageName = fileDir.getName();
                        System.out.println(" ");
                        System.out.println("==> pageName = " + pageName);

                        if (fileDir.listFiles() != null) {
                            dirsFilesCount = fileDir.listFiles().length;
                            System.out.println("--> dirsFilesCount : " + dirsFilesCount);
                            dirs_count = getDirsCount(fileDir.listFiles());
                            System.out.println("--1 dirs_count : " + dirs_count);
                            int files_count =  dirsFilesCount - dirs_count;
                            System.out.println("--2 files_count : " + files_count);
                        }

                        // check if photo files exist
//                        if(beSaved)
                        {
                            if ((dirs_count == 0) && (dirsFilesCount > 0)) {

                                // check if dir has photo files before Save
                                if(getPhotoFilesCount(fileDir)>0) {

                                    // add new folder
                                    if ((pages_count % PAGES_PER_FOLDER) == 0) {
                                        folders_count = (pages_count / PAGES_PER_FOLDER) + 1 ;
                                        if(returnType == CATEGORY_DATA) {
                                            System.out.println("*> add new folder here : " + (pages_count / PAGES_PER_FOLDER) + 1);
                                            category_array.add(String.valueOf(folders_count));
                                        }
                                    }

                                    // list name
                                    if(returnType == PHOTO_DATA) {
                                        listTitle = pageName;
                                    }

                                    pages_count++;
                                }
                            }
                        }

                        // recursive
                        scan_and_save(fileDir.getAbsolutePath(),beSaved,returnType);

                    } // if (fileDir.isDirectory())
                    else
                    {
                        String photoLink =  "file://".concat(fileDir.getPath());
                        String photoName = new File(photoLink).getName();
//                        if(beSaved)
                        {
                            if(returnType == PHOTO_DATA) {
                                Photo photo = new Photo(listTitle,photoLink,photoName);
                                // add photo instance
                                photo_array.add(photo);
                            }
                        }
                    }
                }
            } // for (String file : list)
        } // if (list.size() > 0)

    }


    // Get photo files count
    static int getPhotoFilesCount(File dir)
    {
        int photoFilesCount = 0;
        File[] files = dir.listFiles();
        {
            // sort by alphabetic
            if (files != null) {
                Arrays.sort(files, new FileNameComparator());

                for (File file : files) {
                    // add for filtering non-audio file
                    if (!file.isDirectory() &&
                       (hasImageExtension(file))) {
                        photoFilesCount++;
                    }
                }
            }
        }
//        System.out.println("---------------- photoFilesCount = " + photoFilesCount);
        return  photoFilesCount;
    }

    // get list array in designated path
    static List<String> getListInPath(String path){
        File[] files = new File(path).listFiles();

        filePathArray = new ArrayList<>();
        fileNames = new ArrayList<>();
        filePathArray.add("");

        if(currFilePath.equalsIgnoreCase(new File(docDir).getParent()))
            fileNames.add("ROOT");
        else if(currFilePath.equalsIgnoreCase(docDir))
            fileNames.add("..");
        else
            fileNames.add("..");

        // sort by alphabetic
        if(files != null) {
            Arrays.sort(files, new FileNameComparator());

            for (File file : files) {
                // add for filtering non-audio file
                if (!file.isDirectory() &&
                    (hasImageExtension(file))) {
                    filePathArray.add(file.getPath());
                    // file
                    fileNames.add(file.getName());
                } else if (file.isDirectory()) {
                    filePathArray.add(file.getPath());
                    // directory
                    fileNames.add(file.getName());
                }
            }
        }
        return fileNames;
    }

    // Get directories count
    static int getDirsCount(File[] files){
        int dirCount = 0;
        if(files != null)
        {
            // sort by alphabetic
            Arrays.sort(files, new FileNameComparator());

            for(File file : files)
            {
                if(file.isDirectory())
                    dirCount++;
            }
        }
        return dirCount;
    }

    // check if file has image extension
    // refer to http://developer.android.com/intl/zh-tw/guide/appendix/media-formats.html
    static boolean hasImageExtension(File file){
        boolean has = false;
        String fn = file.getName().toLowerCase(Locale.getDefault());
        if(	fn.endsWith("jpg") || fn.endsWith("gif") ||
            fn.endsWith("png") || fn.endsWith("bmp") || fn.endsWith("webp") )
            has = true;

        return has;
    }

    // Directory group and file group, both directory and file are sorted alphabetically
    // cf. https://stackoverflow.com/questions/24404055/sort-filelist-folders-then-files-both-alphabetically-in-android
    private static class FileNameComparator implements Comparator<File> {
        // lhs: left hand side
        // rhs: right hand side
        public int compare(File lhsS, File rhsS){
            File lhs = new File(lhsS.toString().toLowerCase(Locale.US));
            File rhs= new File(rhsS.toString().toLowerCase(Locale.US));
            if (lhs.isDirectory() && !rhs.isDirectory()){
                // Directory before File
                return -1;
            } else if (!lhs.isDirectory() && rhs.isDirectory()){
                // File after directory
                return 1;
            } else {
                // Otherwise in Alphabetic order...
                return lhs.getName().compareTo(rhs.getName());
            }
        }
    }
    // Create category DB 2
    public static void createCategoryDB2(Activity act){
        System.out.println("LocalData / _createCategoryDB2");
        String docDir;
        List<StorageUtils.StorageInfo> storageList = StorageUtils.getStorageList();

        for (int i = 0; i < storageList.size(); i++) {
            System.out.println("-->  storageList[" + i + "] name = " + storageList.get(i).getDisplayName());
            System.out.println("-->  storageList[" + i + "] path = " + storageList.get(i).path);
            System.out.println("-->  storageList[" + i + "] display number = " + storageList.get(i).display_number);

            docDir = storageList.get(i).path;

            if (docDir.contains("/mnt/media_rw"))
                docDir = docDir.replace("mnt/media_rw", "storage");

            System.out.println("-->  storageList[" + i + "] docDir = " + docDir);

            LocalData.init(docDir);
            LocalData.scan_and_save(docDir, true, LocalData.CATEGORY_DATA);

            List<String> categoryArray = LocalData.category_array;
            List<ContentValues> videosToInsert = new ArrayList<>();

            for (int h = 0; h < categoryArray.size(); h++) {
                String category_name = categoryArray.get(h);
                System.out.println("? ----- category_name = " + category_name);
                // save category names
                ContentValues categoryValues = new ContentValues();
                categoryValues.put("category_name", category_name);
                categoryValues.put("video_table_id", h + 1);
                videosToInsert.add(categoryValues);
            }

            try {
                List<ContentValues> contentValuesList = videosToInsert;

                ContentValues[] downloadedVideoContentValues =
                        contentValuesList.toArray(new ContentValues[contentValuesList.size()]);

                ContentResolver contentResolver = act.getContentResolver();
//            System.out.println("----> contentResolver = " + contentResolver.toString());

                contentResolver.bulkInsert(VideoContract.CategoryEntry.CONTENT_URI, downloadedVideoContentValues);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // create video DB
    public static void createVideoDB2(Activity act){
//        String docDir = Environment.getExternalStorageDirectory().getAbsolutePath()
//                + File.separator + Environment.DIRECTORY_DCIM;
        String docDir;

        List<StorageUtils.StorageInfo> storageList = StorageUtils.getStorageList();

        for (int i = 0; i < storageList.size(); i++) {
            System.out.println("-->  storageList[" + i + "] name = " + storageList.get(i).getDisplayName());
            System.out.println("-->  storageList[" + i + "] path = " + storageList.get(i).path);
            System.out.println("-->  storageList[" + i + "] display number = " + storageList.get(i).display_number);

            docDir = storageList.get(i).path;

            if (docDir.contains("/mnt/media_rw"))
                docDir = docDir.replace("mnt/media_rw", "storage");

            System.out.println("-->  storageList[" + i + "] docDir = " + docDir);

            System.out.println("---- docDir = " + docDir);

            LocalData.init(docDir);

            LocalData.scan_and_save(docDir, true, LocalData.CATEGORY_DATA);
            List<String> categoryArray = LocalData.category_array;

            LocalData.init(docDir);
            LocalData.scan_and_save(docDir, true, LocalData.PHOTO_DATA);
            List<Photo> photoArray = LocalData.photo_array;

            List<ContentValues> videosToInsert = new ArrayList<>();

            int list_number = 0;
            String old_row_title = null;
            for (int j = 0; j < photoArray.size(); j++) {
                String rowTitle = photoArray.get(j).getList_title();

                if ( (rowTitle!=null) &&
                     (!rowTitle.equalsIgnoreCase(old_row_title)) ) {
                    old_row_title = rowTitle;
                    list_number++;
                    System.out.println("-> list_number = " + list_number);
                }

                String photoName = photoArray.get(j).getPhoto_name();
                System.out.println("----- photoName = " + photoName);

                String linkUrl = "https://www.youtube.com/watch?v=h-AJ0ApCjVI";

                // card image Url: YouTube or HTML
                String photoLink;
                photoLink = photoArray.get(j).getPhoto_link();
                System.out.println("----- photoLink = " + photoLink);

                ContentValues videoValues = new ContentValues();
                videoValues.put(VideoContract.VideoEntry.COLUMN_ROW_TITLE, rowTitle);
                videoValues.put(VideoContract.VideoEntry.COLUMN_LINK_TITLE, photoName);
                videoValues.put(VideoContract.VideoEntry.COLUMN_LINK_URL, linkUrl);
                videoValues.put(VideoContract.VideoEntry.COLUMN_THUMB_URL, photoLink);

                if (act != null) {
                    videoValues.put(VideoContract.VideoEntry.COLUMN_ACTION,
                            act.getResources().getString(R.string.global_search));
                }

                videosToInsert.add(videoValues);

                if ((list_number % PAGES_PER_FOLDER) == 0) {
                    int folder_number = (list_number / PAGES_PER_FOLDER);// + 1;
                    doBulkInsert_videoDB(act, folder_number, videosToInsert);
                    videosToInsert = new ArrayList<>();
                } else if (j == photoArray.size() - 1) {
                    int folder_number = categoryArray.size();
                    doBulkInsert_videoDB(act, folder_number, videosToInsert);
                    videosToInsert = new ArrayList<>();
                }
            }
        }

    }

    // Create category DB
//    public static void createCategoryDB(Activity act){
//        System.out.println("LocalData / _createCategoryDB");
//        String docDir = null;
//        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q /*API 29*/){
//            if(Environment.isExternalStorageLegacy()) {
//                System.out.println("LocalData / _createCategoryDB / isExternalStorageLegacy()");
//                docDir = Environment.getExternalStorageDirectory().getAbsolutePath()
//                        + File.separator + Environment.DIRECTORY_DCIM;
//            }
//        } else {
//            docDir = Environment.getExternalStorageDirectory().getAbsolutePath()
//                    + File.separator + Environment.DIRECTORY_DCIM;
//        }
//
//        System.out.println("---- docDir = " + docDir);
//
//        ///
//        // test for MediaStore
////        String[] projection = new String[] {"_id"
//////                media-database-columns-to-retrieve
////        };
////        String selection = null;//sql-where-clause-with-placeholder-variables;
////        String[] selectionArgs = null;
//////        new String[] {
//////                values-of-placeholder-variables
//////        };
////        String sortOrder = null;//sql-order-by-clause;
////
////        Cursor cursor = act.getApplicationContext().getContentResolver().query(
////                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
////                projection,
////                selection,
////                selectionArgs,
////                sortOrder
////        );
////
////        while (cursor.moveToNext()) {
////            // Use an ID column from the projection to get
////            // a URI representing the media item itself.
////            @SuppressLint("Range") String id = cursor.getString(cursor.getColumnIndex("_id"));
////            Uri uri = Uri.withAppendedPath(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
////            System.out.println("uri = " + uri );
////
////            String realPath = getLocalRealPathByUri(act, uri);
////            System.out.println("realPath =  " + realPath );
////        }
////        cursor.close();
//        ///
//
//        LocalData.init(docDir);
//        LocalData.scan_and_save(docDir,true,LocalData.CATEGORY_DATA);
//
//        List<String> categoryArray = LocalData.category_array;
//        List<ContentValues> videosToInsert = new ArrayList<>();
//
//        for (int h = 0; h < categoryArray.size(); h++) {
//            String category_name = categoryArray.get(h);
//            System.out.println("? ----- category_name = " + category_name);
//            // save category names
//            ContentValues categoryValues = new ContentValues();
//            categoryValues.put("category_name", category_name);
//            categoryValues.put("video_table_id", h+1);
//            videosToInsert.add(categoryValues);
//        }
//
//        try {
//            List<ContentValues> contentValuesList = videosToInsert;
//
//            ContentValues[] downloadedVideoContentValues =
//                    contentValuesList.toArray(new ContentValues[contentValuesList.size()]);
//
//            ContentResolver contentResolver = act.getContentResolver();
////            System.out.println("----> contentResolver = " + contentResolver.toString());
//
//            contentResolver.bulkInsert(VideoContract.CategoryEntry.CONTENT_URI, downloadedVideoContentValues);
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }

    // create video DB
//    public static void createVideoDB(Activity act){
//        String docDir = Environment.getExternalStorageDirectory().getAbsolutePath()
//                + File.separator + Environment.DIRECTORY_DCIM;
//
//        LocalData.init(docDir);
//
//        LocalData.scan_and_save(docDir,true,LocalData.CATEGORY_DATA);
//        List<String> categoryArray = LocalData.category_array;
//
//        LocalData.init(docDir);
//        LocalData.scan_and_save(docDir,true,LocalData.PHOTO_DATA);
//        List<Photo> photoArray = LocalData.photo_array;
//
//        // check
////        int size = photoArray.size();
////        System.out.println("--------------- size = " + size);
////        for(int i=0;i< size; i++ ){
////            System.out.println("--------------- list title = " + photoArray.get(i).getList_title() );
////            System.out.println("--------------- photo link = " + photoArray.get(i).getPhoto_link() );
////        }
//
//        List<ContentValues> videosToInsert = new ArrayList<>();
//
//        int list_number = 0;
//        String old_row_title = null;
//        for (int j = 0; j < photoArray.size(); j++) {
//            String rowTitle = photoArray.get(j).getList_title();
//
//            if (!rowTitle.equalsIgnoreCase(old_row_title)) {
//                old_row_title = rowTitle;
//                list_number++;
//                System.out.println("-> list_number = " + list_number);
//            }
//
//            String photoName = photoArray.get(j).getPhoto_name();
//            System.out.println("----- photoName = " + photoName);
//
//            String linkUrl = "https://www.youtube.com/watch?v=h-AJ0ApCjVI";
//
//            // card image Url: YouTube or HTML
//            String photoLink;
//            photoLink = photoArray.get(j).getPhoto_link();
//            System.out.println("----- photoLink = " + photoLink);
//
//            ContentValues videoValues = new ContentValues();
//            videoValues.put(VideoContract.VideoEntry.COLUMN_ROW_TITLE, rowTitle);
//            videoValues.put(VideoContract.VideoEntry.COLUMN_LINK_TITLE, photoName);
//            videoValues.put(VideoContract.VideoEntry.COLUMN_LINK_URL, linkUrl);
//            videoValues.put(VideoContract.VideoEntry.COLUMN_THUMB_URL, photoLink);
//
//            if (act != null) {
//                videoValues.put(VideoContract.VideoEntry.COLUMN_ACTION,
//                        act.getResources().getString(R.string.global_search));
//            }
//
//            videosToInsert.add(videoValues);
//
//            if ((list_number % PAGES_PER_FOLDER) == 0) {
//                int folder_number = (list_number / PAGES_PER_FOLDER);// + 1;
//                doBulkInsert_videoDB(act, folder_number, videosToInsert);
//                videosToInsert = new ArrayList<>();
//            } else if(j == photoArray.size()-1){
//                int folder_number = categoryArray.size();
//                doBulkInsert_videoDB(act, folder_number, videosToInsert);
//                videosToInsert = new ArrayList<>();
//            }
//        }
//
//    }

    // do bulk insert to video table
    static void doBulkInsert_videoDB(Activity act, int folder_number, List<ContentValues> videosToInsert)
    {
        //
        // create new video table
        //
        DbHelper mOpenHelper = new DbHelper(act);
        mOpenHelper.setWriteAheadLoggingEnabled(false);

        // Will call DbHelper.onCreate()first time when WritableDatabase is not created yet
        SQLiteDatabase sqlDb;
        sqlDb = mOpenHelper.getWritableDatabase();
        String tableId = String.valueOf(folder_number); //Id starts from 1

        // Create a new table to hold videos.
        final String SQL_CREATE_VIDEO_TABLE = "CREATE TABLE IF NOT EXISTS " + VideoContract.VideoEntry.TABLE_NAME.concat(tableId) + " (" +
                VideoContract.VideoEntry._ID + " INTEGER PRIMARY KEY," +
                VideoContract.VideoEntry.COLUMN_ROW_TITLE + " TEXT NOT NULL, " +
                VideoContract.VideoEntry.COLUMN_LINK_URL + " TEXT NOT NULL, " + // TEXT UNIQUE NOT NULL will make the URL unique.
                VideoContract.VideoEntry.COLUMN_LINK_TITLE + " TEXT NOT NULL, " +
                VideoContract.VideoEntry.COLUMN_THUMB_URL + " TEXT, " +
                VideoContract.VideoEntry.COLUMN_ACTION + " TEXT NOT NULL " +
                " );";

        // Do the creating of the databases.
        sqlDb.execSQL(SQL_CREATE_VIDEO_TABLE);

        //
        // bulk insert data to video table
        //
        try {
            ContentValues[] videoContentValues = videosToInsert.toArray(new ContentValues[videosToInsert.size()]);

            ContentResolver contentResolver = act.getApplicationContext().getContentResolver();

            VideoProvider.tableId = String.valueOf(folder_number);
            contentResolver.bulkInsert(VideoContract.VideoEntry.CONTENT_URI, videoContentValues);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // get local real path from URI
    public static String getLocalRealPathByUri(Context context, Uri contentUri) {
        Cursor cursor = null;
        try {
            String[] proj = { MediaStore.Images.Media.DATA };
            cursor = context.getContentResolver().query(contentUri,  proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        }
        catch (Exception e){
            return null;
        }
        finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

}
