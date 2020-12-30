package com.dx.anonymousmessenger.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
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
                if(one.get(i).length!=two.get(i).length){
                    return false;
                }
                //compare all fields in string[]
                for(int j=0;j<one.get(i).length;j++){
                    if(!one.get(i)[j].equals(two.get(i)[j])){
                        return false;
                    }
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

    public static byte[] splitOne(byte[] input, int firstLength) {
        byte[] part = new byte[firstLength];
        System.arraycopy(input, 0, part, 0, firstLength);
        return part;
    }

    // From: https://programming.guide/worlds-most-copied-so-snippet.html
    public static strictfp String humanReadableByteCount(long bytes) {
        int unit = 1000;
        long absBytes = bytes == Long.MIN_VALUE ? Long.MAX_VALUE : Math.abs(bytes);
        if (absBytes < unit) return bytes + " B";
        int exp = (int) (Math.log(absBytes) / Math.log(unit));
        long th = (long) Math.ceil(Math.pow(unit, exp) * (unit - 0.05));
        if (exp < 6 && absBytes >= th - ((th & 0xFFF) == 0xD00 ? 51 : 0)) exp++;
        String pre = ("KMGTPE").charAt(exp - 1) + "";
        if (exp > 4) {
            bytes /= unit;
            exp -= 1;
        }
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }
}
