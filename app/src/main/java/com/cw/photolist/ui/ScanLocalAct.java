package com.cw.photolist.ui;

import android.app.Activity;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;

import com.cw.photolist.R;
import com.cw.photolist.data.DbHelper;
import com.cw.photolist.data.PhotoContract;
import com.cw.photolist.data.PhotoProvider;
import com.cw.photolist.define.Define;
import com.cw.photolist.utility.LocalData;
import com.cw.photolist.utility.Pref;

import java.util.Objects;


/**
 * splash screen
 */
public class ScanLocalAct extends Activity {

	TextView messageText;
	private Handler handler;
	int countScan = 3;
	int countFinish = 3;
	Activity act;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.scan_local);
		messageText = findViewById(R.id.scan_local_title);
		act = this;


		// permission is granted
		if(Define.DEFAULT_PHOTO_DIRECTORY == Define.DIR_DCIM)
			LocalData.createDB_DCIM(this);
		else if(Define.DEFAULT_PHOTO_DIRECTORY == Define.DIR_ROOT)
			startScan();
	}

	Runnable runnable_scan;
	// show message and do scan
	void startScan(){
		handler =new Handler();
		// runnable for returning to previous stage
		runnable_scan = () -> {
			countScan --;
			if (countScan == 2)
				messageText.setText(R.string.scan_photo_dir);
			else if(countScan == 0) {
				doScan();

				Pref.setPref_db_is_created(act,true);

				showFinishMessage();
			}
			handler.postDelayed(runnable_scan, 1000);
		};
		// start waiting
		handler.post(runnable_scan);
	}

	// do Scan
	void doScan(){
		// delete database
		try {
			System.out.println("SelectLinkSrcFragment / _startFetchService / will delete DB");
			Objects.requireNonNull(act).deleteDatabase(DbHelper.DATABASE_NAME);

			ContentResolver resolver = act.getContentResolver();
			ContentProviderClient client = resolver.acquireContentProviderClient(PhotoContract.CONTENT_AUTHORITY);
			assert client != null;
			PhotoProvider provider = (PhotoProvider) client.getLocalContentProvider();

			assert provider != null;
			provider.mContentResolver = resolver;
			provider.mOpenHelper.close();

			provider.mOpenHelper = new DbHelper(act);
			provider.mOpenHelper.setWriteAheadLoggingEnabled(false);

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
				client.close();
			else
				client.release();

		} catch (Exception e) {
			e.printStackTrace();
		}

		// create DB
		LocalData.createDB_root(this);

		// remove category name key
		Pref.removePref_category_name(act);

		MainFragment.mCategoryNames = null;
	}

	Runnable runnable_finish;
	// show finish message
	void showFinishMessage(){
		messageText.setText(R.string.db_is_updated);
		handler =new Handler();
		// runnable for returning to previous stage
		runnable_finish = () -> {
			countFinish --;
			if (countFinish == 2)
				messageText.setText(R.string.return_to_previous_stage);
			else if(countFinish == 0)
				finish();

			handler.postDelayed(runnable_finish, 1000);
		};
		// start waiting
		handler.post(runnable_finish);
	}

	@Override
	protected void onPause() {
		super.onPause();
		if(handler != null) {
			handler.removeCallbacks(runnable_scan);
			handler.removeCallbacks(runnable_finish);
		}
		finish();
	}

}