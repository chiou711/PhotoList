package com.cw.photolist.utility;

public class Photo {
   public int getCategory_id() {
      return category_id;
   }

   public void setCategory_id(int category_id) {
      this.category_id = category_id;
   }

   public int getRow_id() {
      return row_id;
   }

   public void setRow_id(int row_id) {
      this.row_id = row_id;
   }

   int category_id;
   int row_id;
   String list_title;
   String photo_link;
   String photo_name;

   public Photo(int category_id,int row_id,String list_title,String photo_link,String photo_name) {
      setCategory_id(category_id);
      setRow_id(row_id);
      setList_title(list_title);
      setPhoto_link(photo_link);
      setPhoto_name(photo_name);
   }

   public String getList_title() {
      return list_title;
   }

   public void setList_title(String list_title) {
      this.list_title = list_title;
   }


   public String getPhoto_link() {
      return photo_link;
   }

   public void setPhoto_link(String photo_link) {
      this.photo_link = photo_link;
   }

   public String getPhoto_name() {
      return photo_name;
   }

   public void setPhoto_name(String photo_name) {
      this.photo_name = photo_name;
   }
}
