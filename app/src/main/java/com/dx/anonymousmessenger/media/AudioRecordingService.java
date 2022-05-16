package com.dx.anonymousmessenger.media;

import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.IBinder;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.dx.anonymousmessenger.DxApplication;
import com.dx.anonymousmessenger.R;
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
    private final int sampleRate = 16000; // 44100 for music
    private final int channelConfig = AudioFormat.CHANNEL_CONFIGURATION_MONO;
    private final int audioFormat = AudioFormat.ENCODING_PCM_8BIT;
    private final int minBufSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);
    private boolean status = true;
    // --Commented out by Inspection (9/27/21, 10:04 AM):AudioManager audioManager;
    private int callTimer = 0;
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


    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (Objects.equals(intent.getAction(), "stop_recording")) {
            outputStream = null;
            status = false;
            stopRecording();
            stopSelf();
            return super.onStartCommand(intent, flags, startId);
        }
        this.address = DbHelper.getFullAddress(intent.getStringExtra("address"),
                (DxApplication) getApplication());
        startRecording();
//        String action = intent.getAction();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        new Thread(() -> {
            try {
                stopRecording();
            } catch (Exception e) {
                e.printStackTrace();
            }
            stopSelf();
        }).start();
    }

    public void stopRecording() {
        status = false;
        if (recorder == null || recorder.getState() == AudioRecord.STATE_UNINITIALIZED) {
            return;
        }
        try {
            recorder.stop();
            recorder.release();
        } catch (Exception ignored) {
        }

        if (outputStream == null) {
            return;
        }
        //put recorded stream into bytes
        if (outputStream.size() >= Runtime.getRuntime().freeMemory()) {
            return;
        }
        if (outputStream.size() <= 0) {
            return;
        }
        byte[] recorded = outputStream.toByteArray();
        if (recorded.length <= 0) {
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
            path = FileHelper.saveFile(recorded, app, filename);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        if (path == null) {
            return;
        }
        //save metadata in encrypted database with reference to encrypted file
        QuotedUserMessage qum = new QuotedUserMessage(app.getHostname(), app.getAccount().getNickname(), time, false, address, filename, path, "audio");
        //send message and get received status
        MessageSender.sendMediaMessage(qum, app, address);
    }

    @SuppressWarnings("BusyWait")
    public void startTiming() {
        try {
            while (status) {
                try {
                    Thread.sleep(1000);
                } catch (Exception ignored) {
                }
                callTimer++;
                Intent gcm_rec = new Intent("recording_action");
                gcm_rec.putExtra("action", "timer");
                gcm_rec.putExtra("time", Utils.getMinutesAndSecondsFromSeconds(callTimer));
                LocalBroadcastManager.getInstance(getApplication()).sendBroadcast(gcm_rec);
            }
        } catch (Exception ignored) {
        }
    }

    public void startRecording() {
        byte[] buffer = new byte[minBufSize];
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, channelConfig, audioFormat, buffer.length);
//            if(recorder.getRecordingState() != AudioRecord.RECORDSTATE_STOPPED ){
//                Handler handler = new Handler(getMainLooper());
//                handler.post(()->{
//                    Toast.makeText(this, R.string.mic_in_use, Toast.LENGTH_SHORT).show();
//                });
//                stopRecording();
//                return;
//            }
        }catch (Exception e){Handler handler = new Handler(getMainLooper());
            handler.post(()-> Toast.makeText(this, R.string.recording_failed, Toast.LENGTH_LONG).show());
            stopRecording();
            return;
        }

        outputStream = new ByteArrayOutputStream();

        new Thread(()->{
            try{
                if(!status){
                    return;
                }
                recorder.startRecording();
                if(recorder.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING ){
                    Handler handler = new Handler(getMainLooper());
                    handler.post(()-> Toast.makeText(this, R.string.mic_in_use, Toast.LENGTH_SHORT).show());
                    stopRecording();
                    return;
                }
                new Thread(this::startTiming).start();
                while(status) {
                    if(buffer.length >= Runtime.getRuntime().freeMemory()){
                        Runtime.getRuntime().gc();
                        if(buffer.length >= Runtime.getRuntime().freeMemory()){
                            status = false;
                            outputStream = null;
                            return;
                        }
                    }
                    //reading data from MIC into buffer
                    recorder.read(buffer, 0, buffer.length);
                    //add buffer to recorded bytes
                    outputStream.write(buffer);
                }
            }catch (Exception e){
                e.printStackTrace();
                recorder.release();
                onDestroy();
//            stopRecording(true);
            }
        }).start();
    }
}
