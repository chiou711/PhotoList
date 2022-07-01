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


import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.cw.photolist.R;
import com.cw.photolist.define.Define;

import java.io.IOException;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class Note extends AppCompatActivity
{
    int mEntryPosition;

    public AppCompatActivity act;

	public static String photoPath;
	RequestOptions options;
	ImageView imageView;
	private Handler handler;
	private int count;

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
	    photoPath = getIntent().getExtras().getString("PHOTO_PATH");
	    System.out.println("Note / _onCreate / photoPath = " + photoPath);
		act = this;

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

	    try {
		    setLayoutView();
	    } catch (IOException e) {
		    e.printStackTrace();
		    System.out.println("Note / _onCreate / exception ");
	    }

	    count = Define.DEFAULT_DISPLAY_DURATION;
	    handler = new Handler();
	    handler.postDelayed(runCountDown,100);

	} //onCreate end

	/**
	 * runnable for counting down
	 */
	private final Runnable runCountDown = new Runnable() {
		public void run() {
			// start count down
			count--;

			if(count>0)
				handler.postDelayed(runCountDown,1000);
			else
			{
				if(handler != null) {
					handler.removeCallbacks(runCountDown);
					handler = null;
				}
				finish();
			}
		}
	};


	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		int newPos;
		System.out.println("Note / _onKeyDown / keyCode = " + keyCode);
		switch (keyCode) {
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

	void setLayoutView() throws IOException {
        System.out.println("Note / _setLayoutView");

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
						imageView.setImageDrawable(act.getResources().getDrawable(R.drawable.movie));
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
		finish();
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

}