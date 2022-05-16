package com.dx.anonymousmessenger.media;

import static com.dx.anonymousmessenger.file.FileHelper.getFile;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;

import com.dx.anonymousmessenger.DxApplication;
import com.dx.anonymousmessenger.util.CallBack;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

public class AudioPlayer {
    private AudioTrack at;
//    public AudioManager audioManager;
    private final DxApplication app;
    private String path;
    private final int sampleRate = 16000 ; // 44100 for music
    private final int channelConfig = AudioFormat.CHANNEL_CONFIGURATION_MONO;
    private final int audioFormat = AudioFormat.ENCODING_PCM_8BIT;
    private final int minBufSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);
    final byte[] receiveData = new byte[minBufSize];
    boolean play = false;
    private CallBack callBack;

    public AudioPlayer(DxApplication app, String path){
        this.app = app;
        this.path = path;
    }

    public void registerCallBack(CallBack callBack){
        this.callBack = callBack;
    }

    public void play(){
        play = true;
        byte[] source = getFile(path,app);
        InputStream is = new ByteArrayInputStream(source);
        setAudioDefaults();
        while(play){
            try {
                int read = is.read(receiveData, 0, receiveData.length);
                if(read<1){
                    //Log.d("ANONYMOUSMESSENGER","done playing");
                    stop(1);
                    break;
                }
                toSpeaker(receiveData);
            } catch (Exception ignored) {
                stop(1);
            }
        }
    }

    public void stop(int n){
        play = false;
        path = null;
        try{
            //Log.d("ANONYMOUSMESSENGER","checking for callback");
            if(callBack!=null){
                //Log.d("ANONYMOUSMESSENGER","found callback");
                callBack.doStuff();
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stop(){
        play = false;
        path = null;
    }

    public void setAudioDefaults(){
//        audioManager = (AudioManager)app.getSystemService(Context.AUDIO_SERVICE);
//        audioManager.requestAudioFocus(null, AudioManager.STREAM_VOICE_CALL, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE);
//        audioManager.setMode(AudioManager.STREAM_MUSIC);
//        audioManager.setSpeakerphoneOn(true);
        at = new AudioTrack(AudioManager.STREAM_MUSIC,sampleRate,channelConfig,audioFormat,receiveData.length,AudioTrack.MODE_STREAM);
    }

    public void toSpeaker(byte[] soundBytes) {
        try {
            at.write(soundBytes, 0, soundBytes.length);
            at.play();
        } catch (Exception ignored) {
            //Log.d("ANONYMOUSMESSENGER","Not working in speakers...");
            //e.printStackTrace();
        }
    }
}
