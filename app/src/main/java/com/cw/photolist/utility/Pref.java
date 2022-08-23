package com.cw.photolist.utility;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.cw.photolist.R;
import com.cw.photolist.define.Define;


public class Pref {
	public static int DB_DELETE = 99;

	// is auto play
	public static boolean isAutoPlay(Context context) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		return sharedPreferences.getBoolean(context.getString(R.string.pref_key_auto_play_switch), true);
	}

	// is cyclic by list
	public static boolean isCyclicByList(Context context) {
		return getCyclicPlayRange(context).equalsIgnoreCase("1")?true:false;
	}

	// is cyclic by category
	public static boolean isCyclicByCategory(Context context) {
		return getCyclicPlayRange(context).equalsIgnoreCase("0")?true:false;
	}

	// get cyclic play range
	public static String getCyclicPlayRange(Context context) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		return sharedPreferences.getString(context.getString(R.string.pref_key_cyclic_play_range), "0");
	}

	// get auto play duration
	public static String getAutoPlayDuration(Context context) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		return sharedPreferences.getString(context.getString(R.string.pref_key_auto_play_duration),
									String.valueOf(Define.DEFAULT_DISPLAY_DURATION ));
	}

	// get preference video table ID
	public static int getPref_video_table_id(Context context)
	{
		String catName = getPref_category_name(context);
		return Utils.getVideoTableId_byCategoryName(context,catName);
	}

	// get preference video table ID
	public static int getPref_video_table_id(Activity context)
	{
		String catName = getPref_category_name(context);
		return Utils.getVideoTableId_byCategoryName(context,catName);
	}

	// set preference category name
	public static void setPref_category_name(Context context, String name ){
		System.out.println("Utils / _setPref_category_name / name = " + name);
		SharedPreferences pref = context.getSharedPreferences("category", 0);
		String keyName = "category_name";
		pref.edit().putString(keyName, name).apply();
	}

	// get preference category name
	public static String getPref_category_name(Context context )
	{
		SharedPreferences pref = context.getSharedPreferences("category", 0);
		String keyName = "category_name";
		return pref.getString(keyName, "no category name"); // folder table Id: default is 1
	}

	// remove key of preference category name
	public static void removePref_category_name(Context context){
		SharedPreferences pref = context.getSharedPreferences("category", 0);
		String keyName = "category_name";
		pref.edit().remove(keyName).apply();
	}

	// set DB is created
	public static void setPref_db_is_created(Context context, boolean isUpdated ){
		String keyName = context.getResources().getString(R.string.pref_key_db_is_created);
		SharedPreferences pref = context.getSharedPreferences("database", 0);
		pref.edit().putBoolean(keyName, isUpdated).apply();
	}

	// get DB is created
	public static boolean getPref_db_is_created(Context context) {
		String keyName = context.getResources().getString(R.string.pref_key_db_is_created);
		SharedPreferences pref = context.getSharedPreferences("database", 0);
		return pref.getBoolean(keyName,false);
	}

	// set splash screen URL
	public static void setPref_splash_screen_url(Context context, String url ){
		System.out.println("Utils / _setPref_splash_screen_url / name = " + url);
		SharedPreferences pref = context.getSharedPreferences("splash_screen", 0);
		String keyName = "splash_screen_url";
		pref.edit().putString(keyName, url).apply();
	}

	// get splash screen URL
	public static String getPref_splash_screen_url(Context context )
	{
		SharedPreferences pref = context.getSharedPreferences("splash_screen", 0);
		String keyName = "splash_screen_url";
		return pref.getString(keyName, null);
	}
}