package com.dx.anonymousmessenger.util;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class Utils {
    public static String formatDateTime(long datetime){
        if(new SimpleDateFormat("MM/dd").format(new Date(datetime)).equals(new SimpleDateFormat("MM/dd").format(new Date().getTime()))){
            return new SimpleDateFormat("HH:mm").format(new Date(datetime));
        }
        return new SimpleDateFormat("MM/dd HH:mm").format(new Date(datetime));
    }

    public static boolean arrayListEquals(List<String[]> one, List<String[]> two){
        if(one==null){
            if(two==null){
                return true;
            }else{
                return false;
            }
        }
        if(one.size()!=two.size()){
            return false;
        }
        for(int i=0;i<one.size();i++){
            try{
                if(!Arrays.deepEquals(one.get(i),two.get(i))){
                    return false;
                }
            }catch (Exception e){
                return false;
            }

        }
        return true;
    }
}
