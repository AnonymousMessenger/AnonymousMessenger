package com.dx.anonymousmessenger.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
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

    public static String getMinutesAndSecondsFromSeconds(int seconds){
        int mins = seconds / 60;
        seconds = seconds - mins * 60;
        return (mins<10?"0"+mins:mins)+":"+(seconds<10?"0"+seconds:seconds);
    }

    public static boolean arrayListEquals(List<String[]> one, List<String[]> two){
        if(one==null){
            return two == null;
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

    public static byte[] getSecretBytes(int size) {
        byte[] secret = new byte[size];
        new SecureRandom().nextBytes(secret);
        return secret;
    }

    public static byte[] join(byte[]... input) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            for (byte[] part : input) {
                baos.write(part);
            }

            return baos.toByteArray();
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    public static byte[][] split(byte[] input, int firstLength, int secondLength) {
        byte[][] parts = new byte[2][];

        parts[0] = new byte[firstLength];
        System.arraycopy(input, 0, parts[0], 0, firstLength);

        parts[1] = new byte[secondLength];
        System.arraycopy(input, firstLength, parts[1], 0, secondLength);

        return parts;
    }
}
