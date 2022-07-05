/*
 * Copyright (c) 2014 The Android Open Source Project
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

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;

import com.cw.photolist.Pref;
import com.cw.photolist.R;
import com.cw.photolist.data.VideoContract;
import com.cw.photolist.util.LocalData;
import com.cw.photolist.util.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;

import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

// test only
/*
 * MainActivity class that loads MainFragment.
 */
public class MainActivity extends LeanbackActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        // start
        System.out.println("-------------------------------------");
        System.out.println("--------New start Main Activity------");
        System.out.println("-------------------------------------");


    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        System.out.println("MainActivity / _onActivityResult");
        if(requestCode == MainFragment.VIDEO_DETAILS_INTENT) {
            if(data != null) {
                int action = data.getIntExtra("KEY_DELETE",0);
                if (action == Pref.DB_DELETE)
                {
                    finish();
                    // start new MainActivity
                    Intent new_intent = new Intent(this, MainActivity.class);
                    new_intent.addFlags(FLAG_ACTIVITY_CLEAR_TASK);
                    new_intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
                    startActivity(new_intent);
                }
            } else
            {
                //do nothing for non-action case
            }
        }
    }

}
