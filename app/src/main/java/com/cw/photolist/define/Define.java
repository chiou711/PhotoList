/*
 * Copyright (C) 2022 CW Chiu
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

package com.cw.photolist.define;

/*
 * Created by CW on 2022/03/01
 * Modified by CW on 2022/08/05
 *
 */
public class Define {

    /***************************************************************************
     * Set release/debug mode
     * - RELEASE_MODE
     * - DEBUG_MODE
     ***************************************************************************/
    public static int CODE_MODE;// could be DEBUG_MODE or RELEASE_MODE
    public static int DEBUG_MODE = 0;
    public static int RELEASE_MODE = 1;
    public final static int DEBUG_DEFAULT = 0;
    public final static int RELEASE_DEFAULT = 1;

    public static int app_build_mode = 0;

    /**
     * Set APP build mode
     */
    public static void setAppBuildMode() {
        /** 1 debug */
        int mode = DEBUG_DEFAULT;

        /** 2 release */
//        int mode  =  Define.RELEASE_DEFAULT;

        setAppBuildMode(mode);
    }

    private static void setAppBuildMode(int appBuildMode) {
        app_build_mode = appBuildMode;

        switch (appBuildMode){
            case DEBUG_DEFAULT:
                CODE_MODE = DEBUG_MODE;
                break;

            case RELEASE_DEFAULT:
                CODE_MODE = RELEASE_MODE;
                break;

            default:
                break;
        }
    }

    // --- setting ---
    // auto play, need to match settings.xml
    public final static boolean DEFAULT_AUTO_PLAY = true;

    // initial category number
    public final static int INIT_CATEGORY_NUMBER = 1;

    // --- time ---
    // count down seconds to play next
    public final static int DEFAULT_COUNT_DOWN_TIME_TO_PLAY_NEXT = 1; //3;

    // display duration
    public final static int DEFAULT_DISPLAY_DURATION = 5; //3;
    public final static int DEFAULT_ONE_SECOND_UNITS = 1000; //for fast/slow adjustment

    // auto play case
    public final static int by_onActivityResult = 1; // with count down dialog
    public final static int by_runnable = 2; // without count down dialog
    public final static int DEFAULT_PLAY_NEXT = by_runnable;

    // photo directory origin
    public static int DIR_ROOT = 1;
    public static int DIR_DCIM = 2;
    public static int DEFAULT_PHOTO_DIRECTORY = DIR_ROOT;

}
