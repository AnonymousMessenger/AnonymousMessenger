package com.dx.anonymousmessenger.call;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.util.Log;

import com.dx.anonymousmessenger.DxApplication;
import com.dx.anonymousmessenger.tor.TorClientSocks4;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

public class CallMaker {

    private AudioTrack at;
    private String address;
    private DxApplication app;
    private boolean weCalled;
    private Socket socket;
    AudioRecord recorder;
    private int sampleRate = 16000 ; // 44100 for music
    private int channelConfig = AudioFormat.CHANNEL_CONFIGURATION_MONO;
    private int audioFormat = AudioFormat.ENCODING_PCM_8BIT;
    int minBufSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);
    private boolean status = true;
    AudioManager audioManager;
    byte[] receiveData = new byte[minBufSize];

    public CallMaker(String address, DxApplication app){
        this.address = address;
        this.app = app;
        this.weCalled = true;
        this.socket = null;

        audioManager = (AudioManager)app.getSystemService(Context.AUDIO_SERVICE);
        audioManager.setMode(AudioManager.MODE_IN_CALL);
        audioManager.setSpeakerphoneOn(true);

        at = new AudioTrack(AudioManager.MODE_IN_CALL,sampleRate,channelConfig,audioFormat,receiveData.length,AudioTrack.MODE_STREAM);
    }

    public CallMaker(String address, DxApplication app, Socket socket){
        this.address = address;
        this.app = app;
        this.weCalled = false;
        this.socket = socket;

        audioManager = (AudioManager)app.getSystemService(Context.AUDIO_SERVICE);
        audioManager.setMode(AudioManager.MODE_IN_CALL);
        audioManager.setSpeakerphoneOn(true);

        at = new AudioTrack(AudioManager.MODE_IN_CALL,sampleRate,channelConfig,audioFormat,receiveData.length,AudioTrack.MODE_STREAM);
    }

    public void start(){
        Log.e("Call MAKER", "starting");
        status = true;
        startStreaming();
    }

    public void stop(){
        status = false;
        if(recorder!=null){
            recorder.release();
        }
        app.setCm(null);
        Log.d("Call MAKER","Recorder released");
    }

    public void startStreaming() {
        Thread streamThread = new Thread(() -> {
            try {
                if(weCalled&&socket==null){
                    socket = new TorClientSocks4().getCallSocket(address,app);
                    if(socket==null){
                        stop();
                        return;
                    }
                    socket.setTcpNoDelay(true);
                    socket.setPerformancePreferences(0,2,1);
                    byte[] buffer = new byte[minBufSize];
                    recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,sampleRate,channelConfig,audioFormat,buffer.length);
                    recorder.startRecording();

                    while(status) {
                        //reading data from MIC into buffer
                        recorder.read(buffer, 0, buffer.length);
//                        if(isLoudEnough(buffer)){
                            socket.getOutputStream().write(buffer,0,buffer.length);
//                            socket.getOutputStream().flush();
                            System.out.println("sent size: " +buffer.length);
//                        }
                    }
                }

                if(socket==null){
                    stop();
                    return;
                }

                if (!weCalled) {
                    new Thread(()->{
                        try {
                            while(status){
                                socket.getInputStream().read(receiveData, 0, receiveData.length);
                                new Thread(()->toSpeaker(receiveData)).start();
                                System.out.println("received size: " +receiveData.length);
                            }
                        } catch (IOException e) {
                            Log.e("receiveData:","receiveData.length: ERROR");
                            e.printStackTrace();
                        }
                    }).start();
                }
            } catch(UnknownHostException e) {
                Log.e("Call MAKER", "UnknownHostException");
                status = false;
            } catch (IOException e) {
                status = false;
                e.printStackTrace();
                Log.e("Call MAKER", "IOException");
            }
        });
        streamThread.start();
    }

    public void toSpeaker(byte[] soundBytes) {
        try {
            at.write(soundBytes, 0, soundBytes.length);
            at.play();
        } catch (Exception e) {
            System.out.println("Not working in speakers...");
            e.printStackTrace();
        }
    }

    public boolean isLoudEnough(byte[] data){
        int bufferSize = receiveData.length;
        double average = 0.0;
        double REFERENCE = 0.00002;
        for (short s : data)
        {
            if(s>0)
            {
                average += Math.abs(s);
            }
            else
            {
                bufferSize--;
            }
        }
        //x=max;
        double x = average/bufferSize;

        recorder.release();

        double db;
        if (x==0){
            Log.e("VOICE LEVEL","ZERO X IS BAD X = "+x);
            return false;
        }
        // calculating the pascal pressure based on the idea that the max amplitude (between 0 and 32767) is
        // relative to the pressure
        double pressure = x/51805.5336; //the value 51805.5336 can be derived from asuming that x=32767=0.6325 Pa and x=1 = 0.00002 Pa (the reference value)

        db = (20 * Math.log10(pressure/REFERENCE));

        if(db>20)
        {
            return true;
        }
        Log.e("VOICE LEVEL","NOT MORE THAN 2 DB : "+db);
        return false;
    }

    public String getAddress() {
        return address;
    }

    public boolean getStatus() {return status;}

}
