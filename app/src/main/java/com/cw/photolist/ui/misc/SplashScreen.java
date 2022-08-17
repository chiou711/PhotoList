package com.cw.photolist.ui.misc;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.cw.photolist.R;
import com.cw.photolist.ui.MainActivity;
import com.cw.photolist.utility.Pref;

import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import androidx.annotation.Nullable;

/**
 * splash screen
 */
public class SplashScreen extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.splash);

		LinearLayout layout = (LinearLayout)findViewById(R.id.splash_layout);
		ImageView imageView= new ImageView(this);
//		String photoUrl = "file:///storage/emulated/0/DCIM/g/1/test0.jpg";
		String photoUrl = Pref.getPref_splash_screen_url(this);

		if(photoUrl == null){
			LinearLayout.LayoutParams viewParamsCenter = new LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.MATCH_PARENT,
					LinearLayout.LayoutParams.MATCH_PARENT);
			imageView.setImageResource(R.drawable.doggy2);
			imageView.setLayoutParams(viewParamsCenter);
		} else {
			setSplashScreenImage(imageView,photoUrl);
		}
		layout.addView(imageView);


		// show splash screen
		Thread timerThread = new Thread(){
			public void run(){
				try{
					sleep(1000);
				}catch(InterruptedException e){
					e.printStackTrace();
				}finally{
					// check server connection
					CheckHttpsConnection checkConnTask = new CheckHttpsConnection();
					checkConnTask.execute();
					while(!checkConnTask.checkIsReady)
						SystemClock.sleep(1000);

					if(checkConnTask.connIsOK) {
						// launch
						Intent intent = new Intent(SplashScreen.this, MainActivity.class);
						startActivity(intent);
					} else {
						// exit
						runOnUiThread(new Runnable() {
							public void run() {
								// exit
								Toast.makeText(SplashScreen.this,"Network connection failed.",Toast.LENGTH_LONG).show();
							}
						});
						finish();
					}
				}
			}
		};
		timerThread.start();
	}

	@Override
	protected void onPause() {
		super.onPause();
		finish();
	}

	// set splash screen image
	void setSplashScreenImage(ImageView imageView, String photoUrl){
		RequestOptions options = new RequestOptions()
				.centerCrop()
				.placeholder(R.drawable.movie)
				.error(R.drawable.movie)
				.diskCacheStrategy(DiskCacheStrategy.ALL)
				.priority(Priority.HIGH);

		Glide.with(this)
				.asBitmap()
				.load(photoUrl)
				.apply(options)
				.into(new CustomTarget<Bitmap>() {
					@Override
					public void onResourceReady(Bitmap resource, Transition<? super Bitmap> transition) {
						imageView.setImageBitmap(resource);
						imageView.buildDrawingCache();
						imageView.setVisibility(View.VISIBLE);
					}

					@Override
					public void onLoadCleared(@Nullable Drawable placeholder) {
					}

					@Override
					public void onLoadFailed(@Nullable Drawable errorDrawable) {
						super.onLoadFailed(errorDrawable);
						imageView.setImageDrawable(getResources().getDrawable(R.drawable.movie));
					}
				});

	}

	class CheckHttpsConnection extends AsyncTask<Void,Integer,Void>
	{
		int code = -1;
		boolean checkIsReady;
		boolean connIsOK;

		@Override
		protected Void doInBackground(Void... voids) {
			checkIsReady = false;
			connIsOK = false;

			// HTTPS POST
			String project = "LiteNote";
//			String urlStr =  "https://" + project + ".ddns.net:8443/"+ project +"Web/client/viewNote_json.jsp";
			String urlStr =  "https://www.google.com";

			try {
				URL url = new URL(urlStr);
				MovieList.trustEveryone();
				HttpsURLConnection connection = ((HttpsURLConnection) url.openConnection());
				connection.connect();
				code = connection.getResponseCode();
				System.out.println("SplashScreen / _doInBackground / code = " + code);
				if (code == 200) {
					// reachable
					checkIsReady = true;
					connIsOK = true;
				} else if(code == 404) {
					checkIsReady = true;
					connIsOK = false;
				}
				connection.disconnect();
			}catch (Exception e)
			{
				// connection refused
				checkIsReady = true;
				connIsOK = false;
				e.printStackTrace();
			}
			return null;
		}
	}

}