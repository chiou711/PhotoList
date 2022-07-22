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

package com.cw.photolist.ui.note;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;


import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.cw.photolist.utility.Pref;
import com.cw.photolist.R;
import com.cw.photolist.data.DbData;
import com.cw.photolist.data.VideoContract;
import com.cw.photolist.define.Define;

import java.io.IOException;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class Note extends AppCompatActivity
{
    int mEntryPosition;
	public static String photoPath;
	RequestOptions options;
	ImageView imageView;
	private Handler handler;
	private int count;
	int focusCatNum;
	String table;
	String column_photo_url;
	int photosCount;
	int max_pos_of_row;
	int min_pos_of_row;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        System.out.println("Note / _onCreate");

	    setContentView(R.layout.note_view_landscape);

	    if (getSupportActionBar() != null) {
		    getSupportActionBar().setDisplayHomeAsUpEnabled(false);
		    getSupportActionBar().hide();
	    }

	    // image view
	    imageView = findViewById(R.id.image_view);
	    imageView.setVisibility(View.VISIBLE);

	    // set current selection
	    mEntryPosition = getIntent().getExtras().getInt("PHOTO_POSITION");
	    System.out.println("Note / _onCreate / mEntryPosition = " + mEntryPosition);

	    focusCatNum = Pref.getPref_video_table_id(getBaseContext());
	    table = VideoContract.VideoEntry.TABLE_NAME.concat(String.valueOf(focusCatNum));
	    column_photo_url = VideoContract.VideoEntry.COLUMN_THUMB_URL;

	    photoPath =  DbData.getDB_link_data(getBaseContext(),table, column_photo_url,mEntryPosition);
	    System.out.println("Note / _onCreate / photoPath = " + photoPath);

		String row_title = DbData.getDB_link_data(getBaseContext(),table,VideoContract.VideoEntry.COLUMN_ROW_TITLE,mEntryPosition);
	    min_pos_of_row = DbData.getDB_min_pos_of_row(getBaseContext(),table,row_title);
		max_pos_of_row = DbData.getDB_max_pos_of_row(getBaseContext(),table,row_title);

	    photosCount = DbData.getPhotosCountInCategory(getBaseContext(),table);

		//cf. Measure and optimize bitmap size using Glide or Picasso
	    // https://proandroiddev.com/measure-and-optimize-bitmap-size-using-glide-or-picasso-3273b4a569cd
	    options = new RequestOptions()
//				.centerCrop()
//			    .centerInside()
//			    .override(1920,1080)
//			    .override(500,500)
//			    .format(DecodeFormat.PREFER_RGB_565) // 2 bytes/pixel
			    .format(DecodeFormat.PREFER_ARGB_8888) // 4 bytes/pixel: default
				.placeholder(R.drawable.movie)
			    .error(R.drawable.movie)
//			    .dontAnimate()
			    .priority(Priority.HIGH)
//			    .transform(CircleCrop()) //todo no function found
			    .diskCacheStrategy(DiskCacheStrategy.ALL)
//			    .diskCacheStrategy(DiskCacheStrategy.DATA)
				.priority(Priority.HIGH);

	    count = Define.DEFAULT_DISPLAY_DURATION;
	    handler = new Handler();

	    if(Define.DEFAULT_PLAY_NEXT == Define.by_onActivityResult)
			handler.postDelayed(runCountDown,100);
	    else if(Define.DEFAULT_PLAY_NEXT == Define.by_runnable)
			handler.postDelayed(runAutoPlay,100);

	} //onCreate end

	/**
	 * runnable for counting down
	 */
	private final Runnable runCountDown = new Runnable() {
		public void run() {
			// start count down
			count--;

			if(mToast==null ) {
				mToast = Toast.makeText(getBaseContext(), R.string.auto_play_on, Toast.LENGTH_SHORT);
				mToast.show();
			}

			if(count>0){

				try {
					setPhotoImage();
				} catch (IOException e) {
					e.printStackTrace();
					System.out.println("Note / _runCountDown / exception ");
				}

				handler.postDelayed(runCountDown,1000);
			} else {
				if(handler != null) {
					handler.removeCallbacks(runCountDown);
					handler = null;
				}
				finish();
			}
		}
	};


	/**
	 * runnable for auto play
	 * */
	private final Runnable runAutoPlay = new Runnable() {
		public void run() {

			if(count == Define.DEFAULT_DISPLAY_DURATION){

				if(mToast == null) {
					mToast = Toast.makeText(getBaseContext(), R.string.auto_play_on, Toast.LENGTH_SHORT);
					mToast.show();
				}

				try {
					setPhotoImage();
				} catch (IOException e) {
					e.printStackTrace();
					System.out.println("Note / _runAutoPlay / exception ");
				}
			}

			count --;

			if(count < 0){
				mEntryPosition++;

				if (Pref.isAutoPlayByList(getBaseContext()) ){
					if (mEntryPosition > max_pos_of_row)
						mEntryPosition = min_pos_of_row;
				}

				if (Pref.isAutoPlayByCategory(getBaseContext()) ){
					if (mEntryPosition >= photosCount)
						mEntryPosition = 0;
				}

				photoPath =  DbData.getDB_link_data(getBaseContext(),table, column_photo_url,mEntryPosition);
				count = Define.DEFAULT_DISPLAY_DURATION;

				if(handler!= null)
					handler.postDelayed(runAutoPlay,100);
			} else {
				if(handler != null)
					handler.postDelayed(runAutoPlay, 1000);
			}
		}
	};


	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		System.out.println("Note / _onKeyDown / keyCode = " + keyCode);
		switch (keyCode) {
			case KeyEvent.KEYCODE_DPAD_LEFT: //21
				setPreviousPhotoImage();
				return true;

			case KeyEvent.KEYCODE_DPAD_RIGHT: //22
				setNextPhotoImage();
				return true;

			case KeyEvent.KEYCODE_MEDIA_PREVIOUS: //88
				return true;

			case KeyEvent.KEYCODE_MEDIA_NEXT: //87
				return true;

			case KeyEvent.KEYCODE_MEDIA_PLAY: //126
				return true;

			case KeyEvent.KEYCODE_MEDIA_PAUSE: //127
				return true;

			case KeyEvent.KEYCODE_BACK:
                onBackPressed();
				return true;

			case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
				return true;

			case KeyEvent.KEYCODE_MEDIA_REWIND:
				return true;

			case KeyEvent.KEYCODE_MEDIA_STOP:
				return true;
		}
		return false;
	}

	void setPhotoImage() throws IOException {
        System.out.println("Note / _setPhotoImage");

//		photoPath = "https://i.imgur.com/DvpvklR.png";

		// cf. https://stackoverflow.com/questions/57584072/simpletarget-is-deprecated-glide
		Glide.with(this)
				.asBitmap()
				.load(photoPath)
				.apply(options)
				.into(new CustomTarget<Bitmap>() {
					@Override
					public void onResourceReady(Bitmap resource,Transition<? super Bitmap> transition) {
						System.out.println("----------- onResourceReady");
						imageView.setImageBitmap(resource);
						imageView.buildDrawingCache();
						imageView.setVisibility(View.VISIBLE);
					}

					@Override
					public void onLoadCleared(@Nullable Drawable placeholder) {
						System.out.println("------ _onLoadCleared");
					}

					@Override
					public void onLoadFailed(@Nullable Drawable errorDrawable) {
						super.onLoadFailed(errorDrawable);
						System.out.println("------ _onLoadFailed");
						imageView.setImageDrawable(getResources().getDrawable(R.drawable.movie));
					}
				});

//		Glide.with(this).load("https://i.imgur.com/DvpvklR.png").into(imageView);
//		Glide.with(this).load(photoPath).into(imageView);
	}

	// avoid exception: has leaked window android.widget.ZoomButtonsController
	@Override
	public void finish() {
		System.out.println("Note / _finish");
//		ViewGroup view = (ViewGroup) getWindow().getDecorView();
//	    view.setBackgroundColor(getResources().getColor(color.background_dark)); // avoid white flash
//	    view.setBackgroundColor(getResources().getColor(R.color.bar_color)); // avoid white flash
//	    view.removeAllViews();

		super.finish();
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		System.out.println("Note / _onSaveInstanceState");
	}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) 
    {
        super.onCreateOptionsMenu(menu);
		System.out.println("Note / _onCreateOptionsMenu");
        return true;
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
    	// called after _onCreateOptionsMenu
        return true;
    }  
    
    // for menu buttons
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // on back pressed
    @Override
    public void onBackPressed() {
		System.out.println("Note / _onBackPressed");
	    if(handler != null) {
		    handler.removeCallbacks(runCountDown);
		    handler = null;
	    }
		finish();
    }

	@Override
	protected void onStop() {
		super.onStop();
		if(handler != null) {
			handler.removeCallbacks(runCountDown);
			handler = null;
		}
	}

	@Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        int maskedAction = event.getActionMasked();
        switch (maskedAction) {
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
    	  	  	 break;

	        case MotionEvent.ACTION_MOVE:
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
	        case MotionEvent.ACTION_CANCEL: 
	        	 break;
        }

        return super.dispatchTouchEvent(event);
    }

	Toast mToast;
	// set previous photo image
	void setPreviousPhotoImage(){

		mToast = Toast.makeText(getBaseContext(), R.string.previous_one, Toast.LENGTH_SHORT);
		mToast.show();

		mEntryPosition--;

		if (Pref.isAutoPlayByList(getBaseContext()) ){
			if (mEntryPosition < min_pos_of_row)
				mEntryPosition = max_pos_of_row;
			else if(mEntryPosition > max_pos_of_row)
				mEntryPosition = min_pos_of_row;
		}

		if (Pref.isAutoPlayByCategory(getBaseContext()) ){
			if (mEntryPosition >= photosCount)
				mEntryPosition = 0;
			else if(mEntryPosition < 0)
				mEntryPosition = photosCount-1;
		}

		photoPath =  DbData.getDB_link_data(getBaseContext(),table, column_photo_url,mEntryPosition);

		if(handler != null) {
			handler.removeCallbacks(runCountDown);
			handler = null;
		}

		try {
			setPhotoImage();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// set next photo image
	void setNextPhotoImage(){

		mToast = Toast.makeText(getBaseContext(), R.string.next_one, Toast.LENGTH_SHORT);
		mToast.show();

		mEntryPosition++;

		if (Pref.isAutoPlayByList(getBaseContext()) ){
			if (mEntryPosition < min_pos_of_row)
				mEntryPosition = max_pos_of_row;
			else if(mEntryPosition > max_pos_of_row)
				mEntryPosition = min_pos_of_row;
		}

		if (Pref.isAutoPlayByCategory(getBaseContext()) ){
			if (mEntryPosition >= photosCount)
				mEntryPosition = 0;
			else if(mEntryPosition < 0)
				mEntryPosition = photosCount-1;
		}

		photoPath =  DbData.getDB_link_data(getBaseContext(),table, column_photo_url,mEntryPosition);

		if(handler != null) {
			handler.removeCallbacks(runCountDown);
			handler = null;
		}

		try {
			setPhotoImage();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
}
