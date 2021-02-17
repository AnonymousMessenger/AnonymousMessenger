package com.dx.anonymousmessenger.media;

import android.app.Service;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.IBinder;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.dx.anonymousmessenger.DxApplication;
import com.dx.anonymousmessenger.db.DbHelper;
import com.dx.anonymousmessenger.file.FileHelper;
import com.dx.anonymousmessenger.messages.MessageSender;
import com.dx.anonymousmessenger.messages.QuotedUserMessage;
import com.dx.anonymousmessenger.util.Utils;

import java.io.ByteArrayOutputStream;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.Objects;

public class AudioRecordingService extends Service {
    AudioRecord recorder;
    private final int sampleRate = 16000 ; // 44100 for music
    private final int channelConfig = AudioFormat.CHANNEL_CONFIGURATION_MONO;
    private final int audioFormat = AudioFormat.ENCODING_PCM_8BIT;
    private final int minBufSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);
    private boolean status = true;
    AudioManager audioManager;
    private int callTimer = 0;
    private boolean timerOn;
    ByteArrayOutputStream outputStream;
    private String address;

    public AudioRecordingService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
//        super.onCreate();
        new Thread(this::startTiming).start();
        outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[minBufSize];
        try{
            recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,sampleRate,channelConfig,audioFormat,buffer.length);
        }catch (Exception e){
//            Toast.makeText(this, R.string.recording_failed,Toast.LENGTH_LONG).show();
            stopRecording();
            return;
        }
        new Thread(()->{
            try{
                recorder.startRecording();
                while(status) {
                    if(outputStream.size() >= Runtime.getRuntime().freeMemory()){
                        status = false;
                        outputStream = null;
                        return;
                    }
                    //reading data from MIC into buffer
                    recorder.read(buffer, 0, buffer.length);
                    //add buffer to recorded bytes
                    outputStream.write(buffer);
                }
            }catch (Exception ignored){
                recorder.release();
                onDestroy();
//            stopRecording(true);
            }
        }).start();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(Objects.equals(intent.getAction(), "stop_recording")){
            status = false;
            outputStream = null;
            stopSelf();
        }
        this.address = DbHelper.getFullAddress(intent.getStringExtra("address"),
                (DxApplication) getApplication());
//        String action = intent.getAction();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        new Thread(()->{
            try {
                stopRecording();
            } catch (Exception e) {
                e.printStackTrace();
            }
            stopSelf();
        }).start();
    }

    public void stopRecording() {
        timerOn = false;
        status = false;
        if(recorder==null || recorder.getState()==AudioRecord.STATE_UNINITIALIZED){
            return;
        }
        recorder.stop();
        recorder.release();
        if(outputStream==null){
            return;
        }
        //put recorded stream into bytes
        /* Total number of processors or cores available to the JVM */
        System.out.println("Available processors (cores): " +
                Runtime.getRuntime().availableProcessors());

        /* Total amount of free memory available to the JVM */
        System.out.println("Free memory (bytes): " +
                Runtime.getRuntime().freeMemory());

        /* This will return Long.MAX_VALUE if there is no preset limit */
        long maxMemory = Runtime.getRuntime().maxMemory();
        /* Maximum amount of memory the JVM will attempt to use */
        System.out.println("Maximum memory (bytes): " +
                (maxMemory == Long.MAX_VALUE ? "no limit" : maxMemory));

        /* Total memory currently in use by the JVM */
        System.out.println("Total memory (bytes): " +
                Runtime.getRuntime().totalMemory());

        System.out.println("buffer size: "+outputStream.size());


        if(outputStream.size() >= Runtime.getRuntime().freeMemory()){
            return;
        }
        if(outputStream.size() <= 0){
            return;
        }
        byte[] recorded = outputStream.toByteArray();
        if(recorded.length <= 0){
            return;
        }
        //get app ready
        DxApplication app = (DxApplication) getApplication();
        //get time ready
        long time = new Date().getTime();
        //save bytes encrypted into a file and get path
        String filename = String.valueOf(time);
        String path = null;
        try {
            path = FileHelper.saveFile(recorded,app,filename);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        if(path==null){
            return;
        }
        //save metadata in encrypted database with reference to encrypted file
        QuotedUserMessage qum = new QuotedUserMessage(app.getHostname(),app.getAccount().getNickname(),time,false,address,filename,path,"audio");
        //send message and get received status
        MessageSender.sendMediaMessage(qum,app,address);
    }

    public void startTiming(){
        try{
            timerOn = true;
            while(timerOn){
                try {
                    Thread.sleep(1000);
                }catch (Exception ignored) {}
                callTimer++;
                Intent gcm_rec = new Intent("recording_action");
                gcm_rec.putExtra("action","timer");
                gcm_rec.putExtra("time",Utils.getMinutesAndSecondsFromSeconds(callTimer));
                LocalBroadcastManager.getInstance(getApplication()).sendBroadcast(gcm_rec);
            }
        }catch (Exception ignored){
            timerOn = false;
        }
    }
}
