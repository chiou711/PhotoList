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

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.cw.photolist.R;
import com.cw.photolist.data.DbData;
import com.cw.photolist.data.PhotoContract;
import com.cw.photolist.utility.Pref;

import java.io.IOException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

public class ManualPhotoFragment extends Fragment
{
    public FragmentActivity act;
	int mEntryPosition;
	public static String photoPath;
	RequestOptions options;
	ImageView imageView;
	int focusCatNum;
	String table;
	String column_photo_url;
	int photosCount;
	int max_pos_of_row;
	int min_pos_of_row;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        System.out.println("PhotoFragment / _onCreate");

	    act = getActivity();

	    Bundle arguments = getArguments();
	    System.out.println("PhotoFragment / _onCreate / photoPath = " + photoPath);
	    mEntryPosition = arguments.getInt("PHOTO_POSITION");
	    focusCatNum = Pref.getPref_video_table_id(act);

	    table = PhotoContract.VideoEntry.TABLE_NAME.concat(String.valueOf(focusCatNum));
	    column_photo_url = PhotoContract.VideoEntry.COLUMN_THUMB_URL;

	    photoPath =  DbData.getDB_link_data(act,table, column_photo_url,mEntryPosition);
	    System.out.println("PhotoFragment / _onCreate / photoPath = " + photoPath);

	    String row_title = DbData.getDB_link_data(act,table, PhotoContract.VideoEntry.COLUMN_ROW_TITLE,mEntryPosition);
	    min_pos_of_row = DbData.getDB_min_pos_of_row(act,table,row_title);
	    max_pos_of_row = DbData.getDB_max_pos_of_row(act,table,row_title);

	    photosCount = DbData.getPhotosCountInCategory(act,table);
		//cf. Measure and optimize bitmap size using Glide or Picasso
	    // https://proandroiddev.com/measure-and-optimize-bitmap-size-using-glide-or-picasso-3273b4a569cd

	    options = new RequestOptions()
			    .format(DecodeFormat.PREFER_ARGB_8888) // 4 bytes/pixel: default
				.placeholder(R.drawable.movie)
			    .error(R.drawable.movie)
			    .priority(Priority.HIGH)
			    .diskCacheStrategy(DiskCacheStrategy.ALL)
				.priority(Priority.HIGH);
	} //onCreate end

	View rootView;
	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		rootView = inflater.inflate(R.layout.photo_view_landscape,container, false);
		imageView = rootView.findViewById(R.id.image_view);
		imageView.setVisibility(View.VISIBLE);
		rootView.setBackgroundColor(Color.BLACK);
		return rootView;
	}

	@Override
	public void onResume() {
		try {
			setPhotoImage();
		} catch (IOException e) {
			e.printStackTrace();
		}
		super.onResume();

		// cf. https://codertw.com/%E7%A8%8B%E5%BC%8F%E8%AA%9E%E8%A8%80/755710/
		getView().setFocusableInTouchMode(true);
		getView().requestFocus();
		getView().setOnKeyListener(new View.OnKeyListener() {
			@Override
			public boolean onKey(View view, int keyCode, KeyEvent keyEvent) {
				if (keyEvent.getAction() == KeyEvent.ACTION_UP) {
					switch (keyCode) {
						case KeyEvent.KEYCODE_DPAD_LEFT: //21
							setPreviousPhotoImage();
							break;

						case KeyEvent.KEYCODE_DPAD_RIGHT: //22
							setNextPhotoImage();
							break;
						case KeyEvent.KEYCODE_BACK: //4
							act.onBackPressed();
							break;
					}
				}
				return true;
			}
		});

	}

	void setPhotoImage() throws IOException {
        System.out.println("PhotoFragment / _setPhotoImage");

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
	}

	Toast toastNext_one;
	// set previous photo image
	void setPreviousPhotoImage(){

		if(toastNext_one == null) {
			toastNext_one = Toast.makeText(act, R.string.previous_one, Toast.LENGTH_SHORT);
			toastNext_one.show();
		}

		mEntryPosition--;

		if (Pref.isCyclicByList(act) ){
			if (mEntryPosition < min_pos_of_row)
				mEntryPosition = max_pos_of_row;
			else if(mEntryPosition > max_pos_of_row)
				mEntryPosition = min_pos_of_row;
		}

		if (Pref.isCyclicByCategory(act) ){
			if (mEntryPosition >= photosCount)
				mEntryPosition = 0;
			else if(mEntryPosition < 0)
				mEntryPosition = photosCount-1;
		}


		photoPath =  DbData.getDB_link_data(act.getBaseContext(),table, column_photo_url,mEntryPosition);

		try {
			setPhotoImage();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// set next photo image
	void setNextPhotoImage(){

		if(toastNext_one == null) {
			toastNext_one = Toast.makeText(act, R.string.next_one, Toast.LENGTH_SHORT);
			toastNext_one.show();
		}

		mEntryPosition++;

		if (Pref.isCyclicByList(act) ){
			if (mEntryPosition > max_pos_of_row)
				mEntryPosition = min_pos_of_row;
		}

		if (Pref.isCyclicByCategory(act) ){
			if (mEntryPosition >= photosCount)
				mEntryPosition = 0;
		}

		photoPath =  DbData.getDB_link_data(act,table, column_photo_url,mEntryPosition);

		try {
			setPhotoImage();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
