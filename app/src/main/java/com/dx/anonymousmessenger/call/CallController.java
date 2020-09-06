package com.dx.anonymousmessenger.call;

import android.content.Context;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.RingtoneManager;
import android.net.Uri;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.dx.anonymousmessenger.CallService;
import com.dx.anonymousmessenger.DxApplication;
import com.dx.anonymousmessenger.R;
import com.dx.anonymousmessenger.db.DbHelper;
import com.dx.anonymousmessenger.tor.TorClientSocks4;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;

public class CallController {

    private AudioTrack at;
    AudioRecord recorder;
    private final int sampleRate = 16000 ; // 44100 for music
    private final int channelConfig = AudioFormat.CHANNEL_CONFIGURATION_MONO;
    private final int audioFormat = AudioFormat.ENCODING_PCM_8BIT;
    private final int minBufSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);
    private boolean status = true;
    AudioManager audioManager;
    byte[] receiveData = new byte[minBufSize];
    private int callTimer = 0;
    private boolean timerOn;

    private Socket incoming;
    private Socket outgoing;
    private String address;
    private boolean answered;

    private MediaPlayer player;

    private DxApplication app;

    public int getCallTimer(){
        return callTimer;
    }

    public String getAddress() {
        return address;
    }

    public boolean isAnswered(){
        return answered;
    }

    public void setSpeakerPhoneOn(boolean on){
        audioManager.setSpeakerphoneOn(on);
    }

    public void setAudioDefaults(){
        audioManager = (AudioManager)app.getSystemService(Context.AUDIO_SERVICE);
        audioManager.requestAudioFocus(null, AudioManager.STREAM_VOICE_CALL, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE);
        audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
        audioManager.setSpeakerphoneOn(false);
        at = new AudioTrack(AudioManager.STREAM_VOICE_CALL,sampleRate,channelConfig,audioFormat,receiveData.length,AudioTrack.MODE_STREAM);
    }

    //for responses to new outgoing calls
    public void setIncoming(Socket incoming, String address) {
        if(!this.address.equals(address)){
            //service.tell user about bad address for incoming
            return;
        }
        app.commandCallService(address,CallService.ACTION_START_OUTGOING_CALL_RESPONSE);
        this.incoming = incoming;
        answerCall(true);
    }

    //for new outgoing calls
    public CallController(String address, DxApplication app){
        // start service and notification and get actions ready
        app.commandCallService(address,CallService.ACTION_START_OUTGOING_CALL);
        this.outgoing = new TorClientSocks4().getCallSocket(address,app, CallService.ACTION_START_INCOMING_CALL);
        this.address = address;
        this.app = app;
        if(outgoing==null){
            stopCall(true);
        }
//        Uri notification = RingtoneManager.getDefaultUri();
//        player = MediaPlayer.create(app, notification);
//        player.setLooping(false);
//        player.start();
    }

    //for new incoming calls
    public CallController(String address, Socket incoming, DxApplication app){
        app.commandCallService(address, app.getString(R.string.NotificationBarManager_call_in_progress));
        this.outgoing = new TorClientSocks4().getCallSocket(address,app, CallService.ACTION_START_OUTGOING_CALL_RESPONSE);
        this.incoming = incoming;
        this.address = address;
        this.app = app;
        if(outgoing==null){
            stopCall(true);
        }
        app.commandCallService(address,CallService.ACTION_START_INCOMING_CALL);

        //add a ringtone here that plays for 45 seconds then hangs up the call
        audioManager = (AudioManager)app.getSystemService(Context.AUDIO_SERVICE);
        audioManager.requestAudioFocus(null, AudioManager.STREAM_VOICE_CALL, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE);
        audioManager.setMode(AudioManager.RINGER_MODE_NORMAL);
        Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        player = MediaPlayer.create(app, notification);
        player.setLooping(false);
        player.start();

        new Thread(()->{
            int count = 0;
            try {
                while (!answered){
                    if(incoming!=null && incoming.isConnected() && count<45){
                        try{
                            Thread.sleep(1000);
                        }catch (Exception ignored) {}
                        count++;
                    }else{
                        player.release();
                        audioManager.setMode(AudioManager.MODE_NORMAL);
                        player = null;
                        stopCall(true);
                    }
                }
            }catch (Exception ignored) {
                try {
                    stopCall(true);
                }catch (Exception ignored2) {}
            }
        }).start();
    }

    //ok false for incoming call that we want to answer
    public void answerCall(boolean ok) {
        answered = true;
        new Thread(()->{
            try {
                DataOutputStream iOut = new DataOutputStream(incoming.getOutputStream());
                DataInputStream oIn =new DataInputStream(outgoing.getInputStream());
                //send answer command
                if(ok){
                    //wait only 45 secs?
                    outgoing.setSoTimeout(45000);
                    //todo play dial sounds
                    String msg = oIn.readUTF();
                    //todo stop playing dial sounds
                    if(!msg.contains("ok")){
                        //call canceled
                        stopCall(true);
                        return;
                    }
                    outgoing.setSoTimeout(0);
                    iOut.writeUTF("ok");
                    iOut.flush();
                    Intent gcm_rec = new Intent("call_action");
                    gcm_rec.putExtra("action","answer");
                    LocalBroadcastManager.getInstance(app).sendBroadcast(gcm_rec);
                }else{
                    player.release();
                    audioManager.setMode(AudioManager.MODE_NORMAL);
                    iOut.writeUTF("ok");
                    iOut.flush();
                    //wait only like 5 secs?
                    outgoing.setSoTimeout(5000);
                    String msg = oIn.readUTF();
                    if(!msg.contains("ok")){
                        //call canceled
                        stopCall(true);
                        return;
                    }
                    outgoing.setSoTimeout(0);
                }
                setAudioDefaults();
                //start timer

                //start streaming
                new Thread(this::startStreaming).start();
                //start listening
                new Thread(this::startListening).start();
                //update notification
            } catch (Exception e) {
                if(player!=null){
                    player.release();
                    audioManager.setMode(AudioManager.MODE_NORMAL);
                }
                stopCall(true);
                e.printStackTrace();
            }
        }).start();
    }

    public void stopCall(){
        audioManager.setMode(AudioManager.MODE_NORMAL);
        if(player!=null){
            player.release();
        }
        timerOn = false;
        status = false;
        try {
            incoming.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            outgoing.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        //stop service and notification and timer and nullify everything
    }

    public void stopCall(boolean stopService){
        if(app==null){
            stopCall();
        }
        if(stopService){
            app.commandCallService(address,"hangup");
        }
    }

    public void startStreaming(){
        byte[] buffer = new byte[minBufSize];
        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,sampleRate,channelConfig,audioFormat,buffer.length);
        recorder.startRecording();
        try{
            while(status) {
                //reading data from MIC into buffer
                recorder.read(buffer, 0, buffer.length);
                outgoing.getOutputStream().write(buffer,0,buffer.length);
                System.out.println("sent size: " +buffer.length);
            }
        }catch (Exception ignored){
            recorder.release();
            stopCall(true);
        }
    }

    public void startListening(){
        try {
            incoming.getInputStream().read(receiveData, 0, receiveData.length);
            toSpeaker(receiveData);
            System.out.println("received size: " +receiveData.length);
            new Thread(this::startTiming).start();
            while(status){
                //noinspection ResultOfMethodCallIgnored
                incoming.getInputStream().read(receiveData, 0, receiveData.length);
                toSpeaker(receiveData);
                System.out.println("received size: " +receiveData.length);
            }
        } catch (Exception e) {
            Log.e("receiveData:","receiveData.length: ERROR");
            e.printStackTrace();
            stopCall(true);
        }
    }

    public void startTiming(){
        try{
            timerOn = true;
            while(timerOn){
                try {
                    Thread.sleep(1000);
                }catch (Exception ignored) {}
                callTimer++;
                Intent gcm_rec = new Intent("call_action");
                gcm_rec.putExtra("action","timer");
                gcm_rec.putExtra("time",callTimer);
                LocalBroadcastManager.getInstance(app).sendBroadcast(gcm_rec);
            }
        }catch (Exception ignored){
            timerOn = false;
        }
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

    public static void callReceiveHandler(Socket sock, DxApplication app) throws Exception {
        Log.e("SERVER CONNECTION", "its a call");
        DataOutputStream outputStream = new DataOutputStream(sock.getOutputStream());
        DataInputStream in=new DataInputStream(sock.getInputStream());
        //todo check to see if busy or some shit
        outputStream.writeUTF("ok");
        outputStream.flush();
        String msg = in.readUTF();
        if(msg.trim().endsWith(".onion")){
            String address= msg.trim();
            //check if we want this guy calling us
            if(!DbHelper.contactExists(address,app)){
                //send hangup
                outputStream.writeUTF("nuf");
                outputStream.flush();
                sock.close();
                return;
            }
            outputStream.writeUTF("ok");
            outputStream.flush();
            msg = in.readUTF();
            sock.setKeepAlive(true);
            if(msg.equals(CallService.ACTION_START_OUTGOING_CALL_RESPONSE)){
                if(!app.isInCall()){
                    //send hangup
                    outputStream.writeUTF("nuf");
                    outputStream.flush();
                    sock.close();
                    return;
                }
                app.getCc().setIncoming(sock,address);
            }else if(msg.equals(CallService.ACTION_START_INCOMING_CALL)){
                if(app.isInCall()){
                    //send hangup
                    outputStream.writeUTF("nuf");
                    outputStream.flush();
                    sock.close();
                    return;
                }
                // tell user n get response ready
                app.setCc(new CallController(address,sock,app));
            }
            return;
        }else{
            outputStream.writeUTF("nuf");
            outputStream.flush();
        }
        outputStream.close();
    }
}
