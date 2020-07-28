package com.example.anonymousmessenger.util;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Utils {
    public static String formatDateTime(long datetime){
        return new SimpleDateFormat("MM/dd/ H:m").format(new Date(datetime));
    }
}
