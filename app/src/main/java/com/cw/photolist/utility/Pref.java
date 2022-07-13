package com.cw.photolist.utility;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.cw.photolist.R;
import com.cw.photolist.define.Define;

import static com.cw.photolist.define.Define.DEFAULT_AUTO_PLAY_BY_CATEGORY;
import static com.cw.photolist.define.Define.DEFAULT_AUTO_PLAY_BY_LIST;
import static com.cw.photolist.define.Define.DEFAULT_SEL_FILE_MGR_APP;

public class Pref {
	public static int DB_DELETE = 99;

	public static boolean isAutoPlayByList(Context context) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		return sharedPreferences.getBoolean(context.getString(R.string.pref_key_auto_play_by_list), DEFAULT_AUTO_PLAY_BY_LIST);
	}

	public static boolean isAutoPlayByCategory(Context context) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		return sharedPreferences.getBoolean(context.getString(R.string.pref_key_auto_play_by_category), DEFAULT_AUTO_PLAY_BY_CATEGORY);
	}

	public static boolean isSelFileMgrApp(Context context) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		return sharedPreferences.getBoolean(context.getString(R.string.pref_key_sel_file_mgr_app), DEFAULT_SEL_FILE_MGR_APP);
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

	// set link source number
	public static void setPref_link_source_number(Context context, int linkSrcNumber ){
		SharedPreferences pref = context.getSharedPreferences("link_src", 0);
		String keyName = "link_source_number";
		pref.edit().putInt(keyName, linkSrcNumber).apply();
	}

	// get link source number
	// Note:  after new installation, link source number
	// 1 dedicated for Default: apply this
	// 2 dedicated for Local: not ready
	public static int getPref_link_source_number (Context context) {
		SharedPreferences pref = context.getSharedPreferences("link_src", 0);
		String keyName = "link_source_number";
		return pref.getInt(keyName, Define.INIT_SOURCE_LINK_NUMBER);
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

}