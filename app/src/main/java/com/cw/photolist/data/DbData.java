package com.cw.photolist.data;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.cw.photolist.utility.Pref;

public class DbData {


   // get DB link data
   public static String getDB_link_data(Context context,
                                        String table,
                                        String columnName,
                                        int pos)   {
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

   // get DB id by position
   public static Long getDB_id_byPosition(Context context,
                                        String table,
                                        String columnName,
                                        int pos)   {
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
      Long retData = cursor.getLong(index);
      cursor.close();
      sqlDb.close();

      return retData;
   }

   // get minimum position of row
   public static int getDB_min_pos_of_row(Context context,
                                        String table,
                                        String row_title)   {
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

      int index = cursor.getColumnIndex(PhotoContract.VideoEntry.COLUMN_ROW_TITLE);

      int row_length = cursor.getCount();

      int min_pos = 999999;
      for(int pos=0; pos<row_length; pos++) {
         cursor.moveToPosition(pos);
         String row_title_get =  cursor.getString(index);
         if(row_title_get.equals(row_title)) {
            if(pos < min_pos)
               min_pos = pos;
         }
      }
      cursor.close();
      sqlDb.close();

      return min_pos;
   }

   // get maximum position of row
   public static int getDB_max_pos_of_row(Context context,
                                          String table,
                                          String row_title)   {
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

      int index = cursor.getColumnIndex(PhotoContract.VideoEntry.COLUMN_ROW_TITLE);

      int row_length = cursor.getCount();

      int max_pos = 0;
      for(int pos=0; pos<row_length; pos++) {
         cursor.moveToPosition(pos);
         String row_title_get =  cursor.getString(index);
         if(row_title_get.equals(row_title)) {
            if(pos > max_pos)
               max_pos = pos;
         }
      }
      cursor.close();
      sqlDb.close();

      return max_pos;
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
      int prefVideoTableId = Pref.getPref_video_table_id(context);
      ContentResolver contentResolver = context.getContentResolver();
      String[] projection = new String[]{"_id", "category_name", "video_table_id"};
      String selection = null;
      String[] selectionArgs = null;
      String sortOrder = null;
      Cursor query = contentResolver.query(PhotoContract.CategoryEntry.CONTENT_URI,projection,selection,selectionArgs,sortOrder);

      // get new position by video table ID
      int new_position=0;
      if (query.moveToFirst()) {
         do {
            String columnStr = PhotoContract.CategoryEntry.COLUMN_VIDEO_TABLE_ID;
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
      int focusCatNum = Pref.getPref_video_table_id(act);
      String table = PhotoContract.VideoEntry.TABLE_NAME.concat(String.valueOf(focusCatNum));

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
