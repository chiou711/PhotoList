/*
 * Copyright (c) 2015 The Android Open Source Project
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

package com.cw.photolist.util;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;

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
    public static List<String> returnArray;
    public static List<Photo> returnPhotoArray;

    // limit for create new folder
    // todo Only 2 pages now
    static int PAGES_PER_FOLDER = 7;

    static Integer folders_count;
    static Integer pages_count = 0;
    static Integer existing_folders_count;
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
        existing_folders_count = 0;//todo Need this?
        returnArray = new ArrayList<>();
        returnPhotoArray = new ArrayList<>();
    }

    static String listName = null;
    // Scan all storage devices and save audio links to DB
    public static void scan_and_save(String currFilePath, boolean beSaved, int returnType){

        System.out.println("?----- scan_and_save");
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
//                            System.out.println("--> dirsFilesCount : " + dirsFilesCount);
                            dirs_count = getDirsCount(fileDir.listFiles());
//                            System.out.println("--1 dirs_count : " + dirs_count);
                            int files_count =  dirsFilesCount - dirs_count;
//                            System.out.println("--2 files_count : " + files_count);
                        }

                        // check if audio files exist
                        if(beSaved) {
                            if ((dirs_count == 0) && (dirsFilesCount > 0)) {

                                // check if dir has audio files before Save
                                if(getPhotoFilesCount(fileDir)>0) {

                                    // add new folder
                                    if ((pages_count % PAGES_PER_FOLDER) == 0) {
                                        folders_count = (pages_count / PAGES_PER_FOLDER) + 1 + existing_folders_count;
                                        if(returnType == CATEGORY_DATA) {
                                            System.out.println("*> add new folder here : " + (pages_count / PAGES_PER_FOLDER) + 1);
                                            //todo Condition to create new category
                                            returnArray.add(String.valueOf(folders_count));
                                        }
                                    }

                                    // add new page
                                    if(returnType == PHOTO_DATA) {
                                        System.out.println("**> add new page here : " + pageName);
//                                        returnArray.add(pageName);
                                        listName = pageName;
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
                        String photoUri =  "file://".concat(fileDir.getPath());
                        if(beSaved) {
                            if(returnType == PHOTO_DATA) {
                                Photo photo = new Photo(listName,photoUri);
                                returnPhotoArray.add(photo);
                                System.out.println("***> add new listName here : " + listName);
                                System.out.println("***> add new photoUri here : " + photoUri);
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
        System.out.println("---------------- photoFilesCount = " + photoFilesCount);
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

    // Create category DB
    public static void createCategoryDB(Activity act){
        String docDir = Environment.getExternalStorageDirectory().getAbsolutePath()
                + File.separator + Environment.DIRECTORY_DCIM;

        LocalData.init(docDir);
        LocalData.scan_and_save(docDir,true,LocalData.CATEGORY_DATA);
        List<String> categoryArray = LocalData.returnArray;

        List<ContentValues> videosToInsert = new ArrayList<>();

        for (int h = 0; h < categoryArray.size(); h++) {
            String category_name = categoryArray.get(h);
            System.out.println("? ----- category_name = " + category_name);
            // save category names
            ContentValues categoryValues = new ContentValues();
            categoryValues.put("category_name", category_name);
            categoryValues.put("video_table_id", h+1);
            videosToInsert.add(categoryValues);
        }

        try {
            List<ContentValues> contentValuesList = videosToInsert;

            ContentValues[] downloadedVideoContentValues =
                    contentValuesList.toArray(new ContentValues[contentValuesList.size()]);

            ContentResolver contentResolver = act.getContentResolver();
            System.out.println("----> contentResolver = " + contentResolver.toString());

            contentResolver.bulkInsert(VideoContract.CategoryEntry.CONTENT_URI, downloadedVideoContentValues);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    // create video DB
    public static void createVideoDB(Activity act){
        String docDir = Environment.getExternalStorageDirectory().getAbsolutePath()
                + File.separator + Environment.DIRECTORY_DCIM;

        LocalData.init(docDir);

        LocalData.scan_and_save(docDir,true,LocalData.CATEGORY_DATA);
        List<String> categoryArray = LocalData.returnArray;

        LocalData.init(docDir);
        LocalData.scan_and_save(docDir,true,LocalData.PHOTO_DATA);
        List<Photo> photoArray = LocalData.returnPhotoArray;

        ///
        // check
//        int size = photoArray.size();
//        System.out.println("--------------- size = " + size);
//        for(int i=0;i< size; i++ ){
//            System.out.println("--------------- list title = " + photoArray.get(i).getList_title() );
//            System.out.println("--------------- photo link = " + photoArray.get(i).getPhoto_link() );
//        }
        ///

        List<ContentValues> videosToInsert = new ArrayList<>();

        for (int j = 0; j < photoArray.size(); j++) {
            String rowTitle = photoArray.get(j).getList_title();
            String linkTitle = "n/a";
            System.out.println("----- linkTitle = " + linkTitle);

            String linkUrl = "https://www.youtube.com/watch?v=h-AJ0ApCjVI";

            // card image Url: YouTube or HTML
            String cardImageUrl;
            cardImageUrl = photoArray.get(j).getPhoto_link();
            System.out.println("----- cardImageUrl = " + cardImageUrl);

            ContentValues videoValues = new ContentValues();
            videoValues.put(VideoContract.VideoEntry.COLUMN_ROW_TITLE, rowTitle);
            videoValues.put(VideoContract.VideoEntry.COLUMN_LINK_TITLE, linkTitle);
            videoValues.put(VideoContract.VideoEntry.COLUMN_LINK_URL, linkUrl);
            videoValues.put(VideoContract.VideoEntry.COLUMN_THUMB_URL, cardImageUrl);

            if (act != null) {
                videoValues.put(VideoContract.VideoEntry.COLUMN_ACTION,
                        act.getResources().getString(R.string.global_search));
            }

            videosToInsert.add(videoValues);
        }

        //todo Condition to create new video table for new category
        //
        // create new video table
        //
        DbHelper mOpenHelper = new DbHelper(act);
        mOpenHelper.setWriteAheadLoggingEnabled(false);

        // Will call DbHelper.onCreate()first time when WritableDatabase is not created yet
        for(int i=1 ; i<=categoryArray.size(); i++){
            SQLiteDatabase sqlDb;
            sqlDb = mOpenHelper.getWritableDatabase();
            String tableId = String.valueOf(i); //Id starts from 1

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

                VideoProvider.tableId = tableId;
                contentResolver.bulkInsert(VideoContract.VideoEntry.CONTENT_URI, videoContentValues);
            } catch (Exception e){
                e.printStackTrace();
            }

        }

    }


}
