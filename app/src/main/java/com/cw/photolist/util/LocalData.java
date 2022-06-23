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

}
