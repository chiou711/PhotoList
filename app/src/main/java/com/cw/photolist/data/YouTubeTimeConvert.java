package com.cw.photolist.data;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class YouTubeTimeConvert {
    /**
     *
     * @param duration
     * @return "01:02:30"
     */
    public static String convertYouTubeDuration(String duration) {

        List<String> patternList = new ArrayList<>();
        patternList.add("'PT'mm'M'ss'S'");
        patternList.add("'PT'hh'H'mm'M'ss'S'");
        patternList.add("'PT'ss'S'");
        patternList.add("'PT'hh'H'ss'S'");
        patternList.add("'PT'mm'M'");
        patternList.add("'P0D'");

        Calendar c = new GregorianCalendar();
        Date d;
        for(int i=0;i<patternList.size();i++) {
            d = getParsedTime(patternList.get(i), duration);

            if(d != null) {
                c.setTime(d);
                break;
            }
        }
        c.setTimeZone(TimeZone.getDefault());

        // check
//        System.out.println("--- c.get(Calendar.HOUR) = " + c.get(Calendar.HOUR));
//        System.out.println("--- c.get(Calendar.MINUTE) = " + c.get(Calendar.MINUTE));
//        System.out.println("--- c.get(Calendar.SECOND) = " + c.get(Calendar.SECOND));

        String time = "";
        if ( c.get(Calendar.HOUR) > 0 ) {
            if ( String.valueOf(c.get(Calendar.HOUR)).length() == 1 ) {
                time += "0" + c.get(Calendar.HOUR);
            }
            else {
                time += c.get(Calendar.HOUR);
            }
            time += ":";
        }
        // test minute
        if ( String.valueOf(c.get(Calendar.MINUTE)).length() == 1 ) {
            time += "0" + c.get(Calendar.MINUTE);
        }
        else {
            time += c.get(Calendar.MINUTE);
        }
        time += ":";
        // test second
        if ( String.valueOf(c.get(Calendar.SECOND)).length() == 1 ) {
            time += "0" + c.get(Calendar.SECOND);
        }
        else {
            time += c.get(Calendar.SECOND);
        }
        return time ;
    }

    // get parsed time
    static Date getParsedTime(String patternStr, String duration){
        DateFormat df = new SimpleDateFormat(patternStr, Locale.US);
        Date d = null;
        try {
            d = df.parse(duration);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return d;
    }

}