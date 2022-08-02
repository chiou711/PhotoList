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
 * Modified by CW on 2022/07/13
 *
 */
public class Define {

    // --- setting ---
    // auto play by list (default: by list)
    public final static boolean DEFAULT_AUTO_PLAY_BY_LIST = true;

    // auto play by category (default: by list)
    public final static boolean DEFAULT_AUTO_PLAY_BY_CATEGORY = false;

    // initial number of default URL: db_source_id_x
    public final static int INIT_SOURCE_LINK_NUMBER = 1;

    // initial category number
    public final static int INIT_CATEGORY_NUMBER = 1;

    // --- time ---
    // count down seconds to play next
    public final static int DEFAULT_COUNT_DOWN_TIME_TO_PLAY_NEXT = 1; //3;

    // display duration
    public final static int DEFAULT_DISPLAY_DURATION = 5; //3;

    // auto play case
    public final static int by_onActivityResult = 1;
    public final static int by_runnable = 2;
    public final static int DEFAULT_PLAY_NEXT = by_runnable;

    // photo directory origin
    public static int DIR_ROOT = 1;
    public static int DIR_DCIM = 2;
    public static int DEFAULT_PHOTO_DIRECTORY = DIR_ROOT;

}
