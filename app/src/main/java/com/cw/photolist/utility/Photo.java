package com.cw.photolist.utility;

public class Photo {
   String list_title;
   String photo_link;
   String photo_name;

   public Photo(String list_title,String photo_link,String photo_name) {
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
