package com.dx.anonymousmessenger.util;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Utils {
    public static String formatDateTime(long datetime){
        if(new SimpleDateFormat("MM/dd").format(new Date(datetime)).equals(new SimpleDateFormat("MM/dd").format(new Date().getTime()))){
            return new SimpleDateFormat("HH:mm").format(new Date(datetime));
        }
        return new SimpleDateFormat("MM/dd HH:mm").format(new Date(datetime));
    }
}
