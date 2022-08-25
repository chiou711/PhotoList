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

package com.cw.photolist.ui.photo;

import android.content.Intent;
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
import com.cw.photolist.model.Video;
import com.cw.photolist.ui.MainFragment;
import com.cw.photolist.ui.PhotoDetailsActivity;
import com.cw.photolist.utility.Pref;
import com.cw.photolist.R;
import com.cw.photolist.data.DbData;
import com.cw.photolist.data.PhotoContract;
import com.cw.photolist.define.Define;

import java.io.IOException;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityOptionsCompat;

public class AutoPhotoAct extends AppCompatActivity
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
        System.out.println("PhotoAct / _onCreate");

	    setContentView(R.layout.photo_view_landscape);

	    if (getSupportActionBar() != null) {
		    getSupportActionBar().setDisplayHomeAsUpEnabled(false);
		    getSupportActionBar().hide();
	    }

	    // image view
	    imageView = findViewById(R.id.image_view);
	    imageView.setVisibility(View.VISIBLE);

	    // set current selection
	    mEntryPosition = getIntent().getExtras().getInt("PHOTO_POSITION");
	    System.out.println("PhotoAct / _onCreate / mEntryPosition = " + mEntryPosition);

	    focusCatNum = Pref.getPref_video_table_id(getBaseContext());
	    table = PhotoContract.VideoEntry.TABLE_NAME.concat(String.valueOf(focusCatNum));
	    column_photo_url = PhotoContract.VideoEntry.COLUMN_THUMB_URL;

	    photoPath =  DbData.getDB_link_data(getBaseContext(),table, column_photo_url,mEntryPosition);
	    System.out.println("PhotoAct / _onCreate / photoPath = " + photoPath);

		String row_title = DbData.getDB_link_data(getBaseContext(),table, PhotoContract.VideoEntry.COLUMN_ROW_TITLE,mEntryPosition);
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

	    count = Integer.valueOf(Pref.getAutoPlayDuration(this));
	    handler = new Handler();

	    if(Define.DEFAULT_PLAY_NEXT == Define.by_onActivityResult)
			handler.postDelayed(runCountDown,100);
	    else if(Define.DEFAULT_PLAY_NEXT == Define.by_runnable)
			handler.postDelayed(runAutoPlay,100);

	} //onCreate end

	// get Video by position
	Video getVideoByPosition(int pos){
		long id = DbData.getDB_id_byPosition(getBaseContext(),table, "_id" ,pos);
		String row_title = DbData.getDB_link_data(getBaseContext(),table, PhotoContract.VideoEntry.COLUMN_ROW_TITLE,pos);
		String photoPath =  DbData.getDB_link_data(getBaseContext(),table, PhotoContract.VideoEntry.COLUMN_THUMB_URL ,pos);
		String title = DbData.getDB_link_data(getBaseContext(),table, PhotoContract.VideoEntry.COLUMN_LINK_TITLE ,pos);
		String bgImageUrl = "android.resource://com.cw.photolist/drawable/scenery";

		Video video = new Video(
				id,
				row_title,
				title,
				null,
				bgImageUrl,
				photoPath
		);
		return video;
	}

	/**
	 * runnable for counting down
	 */
	private final Runnable runCountDown = new Runnable() {
		public void run() {
			// start count down
			count--;

			if(toast ==null ) {
				toast = Toast.makeText(getBaseContext(), R.string.auto_play_on, Toast.LENGTH_SHORT);
				toast.show();
			}

			if(count>0){

				try {
					setPhotoImage();
				} catch (IOException e) {
					e.printStackTrace();
					System.out.println("PhotoAct / _runCountDown / exception ");
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

			// avoid wrong entry position count
			if(handler == null)
				return;

			if(count == Integer.valueOf(Pref.getAutoPlayDuration(AutoPhotoAct.this))){

				if(toast == null) {
					toast = Toast.makeText(getBaseContext(), R.string.auto_play_on, Toast.LENGTH_SHORT);
					toast.show();
				}

				try {
					setPhotoImage();
				} catch (IOException e) {
					e.printStackTrace();
					System.out.println("PhotoAct / _runAutoPlay / exception ");
				}
			}

			count --;

			if(count < 0){
				mEntryPosition++;

				if (Pref.isCyclicByList(getBaseContext()) ){
					if (mEntryPosition > max_pos_of_row)
						mEntryPosition = min_pos_of_row;
				}

				if (Pref.isCyclicByCategory(getBaseContext()) ){
					if (mEntryPosition >= photosCount)
						mEntryPosition = 0;
				}

				photoPath =  DbData.getDB_link_data(getBaseContext(),table, column_photo_url,mEntryPosition);
				count = Integer.valueOf(Pref.getAutoPlayDuration(AutoPhotoAct.this));

				if(handler!= null)
					handler.postDelayed(runAutoPlay,100);
			} else {
				if(handler != null)
					handler.postDelayed(runAutoPlay, Define.DEFAULT_ONE_SECOND_UNITS);
			}
		}
	};


	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		System.out.println("PhotoAct / _onKeyDown / keyCode = " + keyCode);
		switch (keyCode) {
			case KeyEvent.KEYCODE_DPAD_LEFT: //21
				setPreviousPhotoImage();
				return true;

			case KeyEvent.KEYCODE_DPAD_RIGHT: //22
				setNextPhotoImage();
				return true;

			case KeyEvent.KEYCODE_DPAD_CENTER: //23
				stopAutoPlayNext();
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
        System.out.println("PhotoAct / _setPhotoImage");

//		photoPath = "https://i.imgur.com/DvpvklR.png";

		// cf. https://stackoverflow.com/questions/57584072/simpletarget-is-deprecated-glide
		if(!isDestroyed()) {//fix: You cannot start a load for a destroyed activity
			Glide.with(this)
					.asBitmap()
					.load(photoPath)
					.apply(options)
					.into(new CustomTarget<Bitmap>() {
						@Override
						public void onResourceReady(Bitmap resource, Transition<? super Bitmap> transition) {
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
		}

//		Glide.with(this).load("https://i.imgur.com/DvpvklR.png").into(imageView);
//		Glide.with(this).load(photoPath).into(imageView);
	}

	// avoid exception: has leaked window android.widget.ZoomButtonsController
	@Override
	public void finish() {
		System.out.println("PhotoAct / _finish");
//		ViewGroup view = (ViewGroup) getWindow().getDecorView();
//	    view.setBackgroundColor(getResources().getColor(color.background_dark)); // avoid white flash
//	    view.setBackgroundColor(getResources().getColor(R.color.bar_color)); // avoid white flash
//	    view.removeAllViews();

		super.finish();
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		System.out.println("PhotoAct / _onSaveInstanceState");
	}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) 
    {
        super.onCreateOptionsMenu(menu);
		System.out.println("PhotoAct / _onCreateOptionsMenu");
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
		System.out.println("PhotoAct / _onBackPressed");
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

	Toast toast;
	Toast toast_next_one;
	Toast toast_stop_auto;
	// set previous photo image
	void setPreviousPhotoImage(){

		if(toast_next_one == null) {
			toast_next_one = Toast.makeText(getBaseContext(), R.string.previous_one, Toast.LENGTH_SHORT);
			toast_next_one.show();
		}

		mEntryPosition--;

		if (Pref.isCyclicByList(getBaseContext()) ){
			if (mEntryPosition < min_pos_of_row)
				mEntryPosition = max_pos_of_row;
			else if(mEntryPosition > max_pos_of_row)
				mEntryPosition = min_pos_of_row;
		}

		if (Pref.isCyclicByCategory(getBaseContext()) ){
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

		if(toast_next_one == null) {
			toast_next_one = Toast.makeText(getBaseContext(), R.string.next_one, Toast.LENGTH_SHORT);
			toast_next_one.show();
		}

		mEntryPosition++;

		if (Pref.isCyclicByList(getBaseContext()) ){
			if (mEntryPosition < min_pos_of_row)
				mEntryPosition = max_pos_of_row;
			else if(mEntryPosition > max_pos_of_row)
				mEntryPosition = min_pos_of_row;
		}

		if (Pref.isCyclicByCategory(getBaseContext()) ){
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

	// stop auto play next
	void stopAutoPlayNext(){
		if(handler != null) {
			handler.removeCallbacks(runCountDown);
			handler = null;
		} else {
			// launch PhotoDetailsActivity
			Video video = getVideoByPosition(mEntryPosition);
			Intent intent = new Intent(AutoPhotoAct.this, PhotoDetailsActivity.class);
			intent.putExtra(PhotoDetailsActivity.VIDEO, video);

			runOnUiThread(new Runnable() {
				public void run() {
					Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(
							AutoPhotoAct.this,
							imageView,
							PhotoDetailsActivity.SHARED_ELEMENT_NAME).toBundle();
					startActivityForResult(intent, MainFragment.VIDEO_DETAILS_INTENT, bundle);
				}
			});
		}

		if(toast_stop_auto == null) {
			toast_stop_auto = Toast.makeText(getBaseContext(), R.string.auto_play_off, Toast.LENGTH_SHORT);
			toast_stop_auto.show();
		}
	}
}
