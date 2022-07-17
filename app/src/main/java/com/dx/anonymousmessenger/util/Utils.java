package com.dx.anonymousmessenger.util;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Build;
import android.text.util.Linkify;
import android.view.View;
import android.widget.TextView;

import androidx.exifinterface.media.ExifInterface;

import com.dx.anonymousmessenger.R;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.text.CharacterIterator;
import java.text.SimpleDateFormat;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Utils {
    @SuppressLint("SimpleDateFormat")
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

    public static String secToTime(int sec) {
        int second = sec % 60;
        int minute = sec / 60;
        if (minute >= 60) {
            int hour = minute / 60;
            minute %= 60;
            return (hour<10?"0"+hour:hour) + ":" + (minute < 10 ? "0" + minute : minute) + ":" + (second < 10 ? "0" + second : second);
        }
        return (minute<10?"0"+minute:minute) + ":" + (second < 10 ? "0" + second : second);
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
                    if(one.get(i)[j]==null || two.get(i)[j]==null){
                        if(one.get(i)[j]==null && two.get(i)[j]==null){
                            continue;
                        }
                        return false;
                    }else{
                        if(!one.get(i)[j].equals(two.get(i)[j])){
                            return false;
                        }
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

    @SuppressLint("DefaultLocale")
    public static String humanReadableByteCountBin(long bytes) {
        long absB = bytes == Long.MIN_VALUE ? Long.MAX_VALUE : Math.abs(bytes);
        if (absB < 1024) {
            return bytes + " B";
        }
        long value = absB;
        CharacterIterator ci = new StringCharacterIterator("KMGTPE");
        for (int i = 40; i >= 0 && absB > 0xfffccccccccccccL >> i; i -= 10) {
            value >>= 10;
            ci.next();
        }
        value *= Long.signum(bytes);
        return String.format("%.1f %ciB", value / 1024.0, ci.current());
    }


    // From: https://programming.guide/worlds-most-copied-so-snippet.html
    @SuppressLint("DefaultLocale")
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

    public static String humanReadableSpeed(int length, long time) {
        /* download rate in bits per second */
        float bitsPerSec = (length*8)
                / ((System.currentTimeMillis() - time) / (float)1000);
        float mbPerSec = bitsPerSec / (1024) / (1024);
        String str;
        if((mbPerSec+"").split("\\.").length>0){
            str = (mbPerSec+"").split("\\.")[0]+".";
            if((mbPerSec+"").split("\\.").length>1){
                String part2 = (mbPerSec+"").split("\\.")[1];
                str += part2.length()>2?part2.substring(0,2):part2;
            }
        }else{
            return mbPerSec+"";
        }

        return str+" mbps";
    }

    //takes a file size such as: '3gb' and returns the size in bytes
    public static long parseFileSize(String fileSize){
        String substring = fileSize.trim().substring(0, fileSize.length() - 2).trim();
        long n = Integer.parseInt(substring);
        if(fileSize.endsWith("gb") || fileSize.endsWith("GB")){
            n = n * 1024*1024*1024;
        }else if(fileSize.endsWith("mb") || fileSize.endsWith("MB")){
            n = n * 1024*1024;
        }else if(fileSize.endsWith("kb") || fileSize.endsWith("KB")){
            n = n * 1024;
        }
//        Log.d("ANONYMOUSMESSENGER","file size in bytes is : " + n);
        return n;
    }

    public static Bitmap rotateBitmap(Bitmap bm, String path){
        ExifInterface exif = null;
        int orientation = 1;
        try {
            exif = new ExifInterface(path);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (exif != null) {
            orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);
        }

        Matrix matrix = new Matrix();
        switch (orientation) {
            case 2:
                matrix.setScale(-1, 1);
                break;
            case 3:
                matrix.setRotate(180);
                break;
            case 4:
                matrix.setRotate(180);
                matrix.postScale(-1, 1);
                break;
            case 5:
                matrix.setRotate(90);
                matrix.postScale(-1, 1);
                break;
            case 6:
                matrix.setRotate(90);
                break;
            case 7:
                matrix.setRotate(-90);
                matrix.postScale(-1, 1);
                break;
            case 8:
                matrix.setRotate(-90);
                break;
            default:
                return bm;
        }
        try {
            Bitmap oriented = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), matrix, true);
            bm.recycle();
            return oriented;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bm;
    }

    public static Bitmap rotateBitmap(Bitmap bm, InputStream is){
        ExifInterface exif = null;
        int orientation = 1;
        try {
            exif = new ExifInterface(is);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (exif != null) {
            orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);
        }

        Matrix matrix = new Matrix();
        switch (orientation) {
            case 2:
                matrix.setScale(-1, 1);
                break;
            case 3:
                matrix.setRotate(180);
                break;
            case 4:
                matrix.setRotate(180);
                matrix.postScale(-1, 1);
                break;
            case 5:
                matrix.setRotate(90);
                matrix.postScale(-1, 1);
                break;
            case 6:
                matrix.setRotate(90);
                break;
            case 7:
                matrix.setRotate(-90);
                matrix.postScale(-1, 1);
                break;
            case 8:
                matrix.setRotate(-90);
                break;
            default:
                return bm;
        }
        try {
            Bitmap oriented = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), matrix, true);
            bm.recycle();
            return oriented;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bm;
    }

    public static void showHelpAlert(Context context, String helpText, String helpTitle) {
        TextView help = new TextView(context);
        help.setText(helpText);
        help.setPadding(10,10,10,10);
        help.setTextIsSelectable(true);
        Linkify.addLinks(help, Linkify.ALL);
        View view = new View(context);
        ArrayList<View> viewArrayList = new ArrayList<>();
        viewArrayList.add(help);
        view.addChildrenForAccessibility(viewArrayList);
        android.app.AlertDialog.Builder builder =
                new android.app.AlertDialog.Builder(context,R.style.AppAlertDialog).
                        setMessage(helpTitle).
                        setPositiveButton(R.string.ok, (dialog, which) -> dialog.dismiss()).
                        setView(help);
        builder.create().show();

//        new android.app.AlertDialog.Builder(getContext(),R.style.AppAlertDialog)
//                .setTitle(R.string.action_clear_tor_cache)
//                .setMessage(R.string.clear_tor_cache_explain)
//                .setIcon(android.R.drawable.ic_dialog_alert)
//                .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> new Thread(()->{
//                    try {
//                        ((DxApplication) requireActivity().getApplication()).getAndroidTorRelay().clearTorCache();
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//                }).start())
//                .setNegativeButton(android.R.string.no, (dialog, whichButton)-> {} ).show();
    }

    public static boolean isValidAddress(String address){
        return address.trim().length() == 62 && address.trim().endsWith(".onion");
    }


    public static void getCameraPerms(Activity activity, int requestCode){
//        if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
//            new AlertDialog.Builder(getApplicationContext(),R.style.AppAlertDialog)
//                .setTitle(R.string.cam_perm_ask_title)
//                .setMessage(R.string.why_need_cam)
//                .setIcon(android.R.drawable.ic_dialog_alert)
//                .setPositiveButton(R.string.ask_for_cam_btn, (dialog, which) -> requestPermissions(
//                        new String[] { Manifest.permission.CAMERA },
//                        CAMERA_REQUEST_CODE))
//                .setNegativeButton(R.string.no_thanks, (dialog, which) -> {
//                });
//        } else {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            activity.requestPermissions(
                new String[] { Manifest.permission.CAMERA },
                    requestCode);
        }
//        }
    }
}
