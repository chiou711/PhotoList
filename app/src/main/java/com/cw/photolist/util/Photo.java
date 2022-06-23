package com.cw.photolist.util;

public class Photo {
   String list_title;
   String photo_link;

   public Photo() {
   }

   public Photo(String list_title,String photo_link) {
      setList_title(list_title);
      setPhoto_link(photo_link);
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

}
