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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.cw.photolist.R;

import java.io.IOException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

public class NoteFragment extends Fragment
{
    public FragmentActivity act;

	public static String photoPath;
	RequestOptions options;
	ImageView imageView;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        System.out.println("Note / _onCreate");

	    act = getActivity();

	    Bundle arguments = getArguments();
	    photoPath = arguments.getString("KEY_PHOTO_PATH");
	    System.out.println("Note / _onCreate / photoPath = " + photoPath);

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
		rootView = inflater.inflate(R.layout.note_view_landscape,container, false);
		imageView = rootView.findViewById(R.id.image_view);
		imageView.setVisibility(View.VISIBLE);
		return rootView;
	}

	@Override
	public void onResume() {
		try {
			setLayoutView();
		} catch (IOException e) {
			e.printStackTrace();
		}
		super.onResume();
	}

	void setLayoutView() throws IOException {
        System.out.println("NoteFragment / _setLayoutView");

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
	}

}
