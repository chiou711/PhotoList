package com.cw.photolist.data;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.cw.photolist.util.Utils;

public class DbData {


   // get DB link data
   public static String getDB_link_data(Context context, String table, String columnName, int pos)
   {
      DbHelper mOpenHelper = new DbHelper(context);
      mOpenHelper.setWriteAheadLoggingEnabled(false);
      SQLiteDatabase sqlDb = mOpenHelper.getReadableDatabase();
      Cursor cursor = sqlDb.query(
              table,
              null,//projection,
              null,//selection,
              null,//selectionArgs,
              null,
              null,
              null//sortOrder
      );

      int index = cursor.getColumnIndex(columnName);
      cursor.moveToPosition((int) pos);
      String retData = cursor.getString(index);
      cursor.close();
      sqlDb.close();

      return retData;
   }


   // get photos count in category
   public static int getPhotosCountInCategory(Context context,String table) {
      DbHelper mOpenHelper = new DbHelper(context);
      mOpenHelper.setWriteAheadLoggingEnabled(false);
      SQLiteDatabase sqlDb = mOpenHelper.getReadableDatabase();
      Cursor cursor = sqlDb.query(
              table,
              null,//projection,
              null,//selection,
              null,//selectionArgs,
              null,
              null,
              null//sortOrder
      );

      int count = cursor.getCount();
      cursor.close();
      sqlDb.close();

      return count;
   }

   // get focus position of category row
   public static int getFocusItemPosition_categoryRow(Context context){
      // get current video* tables
      int prefVideoTableId = Utils.getPref_video_table_id(context);
      ContentResolver contentResolver = context.getContentResolver();
      String[] projection = new String[]{"_id", "category_name", "video_table_id"};
      String selection = null;
      String[] selectionArgs = null;
      String sortOrder = null;
      Cursor query = contentResolver.query(VideoContract.CategoryEntry.CONTENT_URI,projection,selection,selectionArgs,sortOrder);

      // get new position by video table ID
      int new_position=0;
      if (query.moveToFirst()) {
         do {
            String columnStr = VideoContract.CategoryEntry.COLUMN_VIDEO_TABLE_ID;
            int index = query.getColumnIndex(columnStr);
            int pointedVideoTableId = query.getInt(index);
            if(pointedVideoTableId == prefVideoTableId)
               break;
            else
               new_position++;

         } while (query.moveToNext());
      }
      query.close();
      return new_position;
   }

   // get cursor position by ID
   public static int getCursorPositionById(Context act,int id){
      int focusCatNum = Utils.getPref_video_table_id(act);
      String table = VideoContract.VideoEntry.TABLE_NAME.concat(String.valueOf(focusCatNum));

      int pos = 0;
      DbHelper mOpenHelper = new DbHelper(act);
      mOpenHelper.setWriteAheadLoggingEnabled(false);
      SQLiteDatabase sqlDb = mOpenHelper.getReadableDatabase();
      Cursor cursor = sqlDb.query(
              table,
              null,//projection,
              null,//selection,
              null,//selectionArgs,
              null,
              null,
              null//sortOrder
      );

      int index_id = cursor.getColumnIndex("_id");
      for(int position=0;position<cursor.getCount();position++){
         cursor.moveToPosition((int) position);
         if(id == cursor.getInt(index_id)) {
            pos = position;
            break;
         }
      }
      cursor.close();
      sqlDb.close();

      return  pos;
   }

}
