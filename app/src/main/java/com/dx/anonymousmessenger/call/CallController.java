package com.dx.anonymousmessenger.call;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.RingtoneManager;
import android.media.audiofx.AcousticEchoCanceler;
import android.media.audiofx.NoiseSuppressor;
import android.net.Uri;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.dx.anonymousmessenger.DxApplication;
import com.dx.anonymousmessenger.R;
import com.dx.anonymousmessenger.db.DbHelper;
import com.dx.anonymousmessenger.messages.MessageEncryptor;
import com.dx.anonymousmessenger.messages.QuotedUserMessage;
import com.dx.anonymousmessenger.tor.TorClient;

import org.whispersystems.libsignal.SignalProtocolAddress;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import codec2.Jcodec2;
//import com.ustadmobile.codec2.Codec2;

public class CallController {

    final Jcodec2 codec2 = new Jcodec2( Jcodec2.CODEC2_MODE_1300 );
    private AudioTrack at;
    AudioRecord recorder;
    private final int sampleRate = 8000; // 44100 for music
    private final int channelConfig = AudioFormat.CHANNEL_IN_MONO;
//    private final int outChannelConfig = AudioFormat.CHANNEL_OUT_MONO;
    private final int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
    private final int minBufSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);
    private final int outMinBufSize = AudioTrack.getMinBufferSize(sampleRate,AudioFormat.CHANNEL_OUT_MONO,AudioFormat.ENCODING_PCM_16BIT);
    short threshold=500;
    private boolean status = true;
    private AudioManager audioManager;
//    private final byte[] receiveData = new byte[minBufSize];
    private int callTimer = 0;
    private boolean timerOn;

    private Socket incoming;
    private final Socket outgoing;
    private final String address;
    private boolean answered;
    private boolean mute;

    private MediaPlayer player;

    private final DxApplication app;

    public int getCallTimer() {
        return callTimer;
    }

    public String getAddress() {
        return address;
    }

    public boolean isAnswered() {
        return answered;
    }

    public void setMuteMic(boolean mute) {
        this.mute = mute;
    }

    public void setSpeakerPhoneOn(boolean on) {
        audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
        audioManager.setSpeakerphoneOn(on);
    }

    @SuppressLint("NewApi")
    public void setAudioDefaults() {
        audioManager = (AudioManager) app.getSystemService(Context.AUDIO_SERVICE);
        audioManager.requestAudioFocus(null, AudioManager.STREAM_VOICE_CALL, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE);
        audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
        audioManager.setSpeakerphoneOn(false);
        at = new AudioTrack.Builder()
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build())
                .setAudioFormat(new AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build())
                .setTransferMode(AudioTrack.MODE_STREAM)
                .setBufferSizeInBytes(10 * outMinBufSize)
                .build();
//        at = new AudioTrack(AudioManager.STREAM_VOICE_CALL, sampleRate, outChannelConfig, audioFormat, outMinBufSize, AudioTrack.MODE_STREAM);
    }

    //for responses to new outgoing calls
    public void setIncoming(Socket incoming, String address) {
        if (!this.address.equals(address)) {
            //service.tell user about bad address for incoming
            return;
        }
        app.commandCallService(address, DxCallService.ACTION_START_OUTGOING_CALL_RESPONSE);
        this.incoming = incoming;
        answerCall(true);
    }

    //for new outgoing calls
    public CallController(String address, DxApplication app) {
        // start service and notification and get actions ready
        app.commandCallService(address, DxCallService.ACTION_START_OUTGOING_CALL);
        this.outgoing = TorClient.getCallSocket(address, app, DxCallService.ACTION_START_INCOMING_CALL);
        this.address = address;
        this.app = app;
        if (outgoing == null) {
            stopCall(true);
        }
//        Uri notification = RingtoneManager.getDefaultUri();
//        player = MediaPlayer.create(app, notification);
//        player.setLooping(false);
//        player.start();
    }

    //for new incoming calls
    public CallController(String address, Socket incoming, DxApplication app) {
        app.commandCallService(address, app.getString(R.string.NotificationBarManager_call_in_progress));
        this.outgoing = TorClient.getCallSocket(address, app, DxCallService.ACTION_START_OUTGOING_CALL_RESPONSE);
        this.incoming = incoming;
        this.address = address;
        this.app = app;
        if (outgoing == null) {
            stopCall(true);
        }
        app.commandCallService(address, DxCallService.ACTION_START_INCOMING_CALL);

        //add a ringtone here that plays for 45 seconds then hangs up the call
        audioManager = (AudioManager) app.getSystemService(Context.AUDIO_SERVICE);
        audioManager.requestAudioFocus(null, AudioManager.STREAM_VOICE_CALL, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE);
        audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
        audioManager.setSpeakerphoneOn(true);
        Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        player = MediaPlayer.create(app, notification);
        player.setLooping(false);
        player.start();

        new Thread(() -> {
            int count = 0;
            try {
                while (!answered) {
                    if (incoming != null && incoming.isConnected() && count < 45) {
                        try {
                            Thread.sleep(1000);
                        } catch (Exception ignored) {
                        }
                        count++;
                    } else {
                        player.release();
                        audioManager.setMode(AudioManager.MODE_NORMAL);
                        player = null;
                        stopCall(true);
                    }
                }
            } catch (Exception ignored) {
                try {
                    stopCall(true);
                } catch (Exception ignored2) {
                }
            }
        }).start();
    }

    //ok false for incoming call that we want to answer
    public void answerCall(boolean ok) {
        answered = true;
        new Thread(() -> {
            try {
                DataOutputStream iOut = new DataOutputStream(incoming.getOutputStream());
                DataInputStream oIn = new DataInputStream(outgoing.getInputStream());
                //send answer command
                if (ok) {
                    //wait only 45 secs?
                    outgoing.setSoTimeout(45000);
                    //todo play dial sounds
                    String msg = oIn.readUTF();
                    //todo stop playing dial sounds
                    if (!msg.contains("ok")) {
                        //call canceled
                        stopCall(true);
                        return;
                    }
                    outgoing.setSoTimeout(0);
                    iOut.writeUTF("ok");
                    iOut.flush();
                    Intent gcm_rec = new Intent("call_action");
                    gcm_rec.putExtra("action", "answer");
                    LocalBroadcastManager.getInstance(app).sendBroadcast(gcm_rec);
                } else {
                    player.release();
                    audioManager.setMode(AudioManager.MODE_NORMAL);
                    iOut.writeUTF("ok");
                    iOut.flush();
                    //wait only like 5 secs?
                    outgoing.setSoTimeout(5000);
                    String msg = oIn.readUTF();
                    if (!msg.contains("ok")) {
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
                if (player != null) {
                    player.release();
                    audioManager.setMode(AudioManager.MODE_NORMAL);
                }
                stopCall(true);
                e.printStackTrace();
            }
        }).start();
    }

    public void stopCall() {
        if (audioManager != null) {
            audioManager.setMode(AudioManager.MODE_NORMAL);
        }
        if (player != null) {
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

    public void stopCall(boolean stopService) {
        if (app == null) {
            stopCall();
        }
        if (stopService) {
            if (app != null) {
                app.commandCallService(address, "hangup");
            }
        }
    }

    public void startStreaming() {

        if (ActivityCompat.checkSelfPermission(app, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, channelConfig, audioFormat, 10*minBufSize);
        if (NoiseSuppressor.isAvailable()) {
            NoiseSuppressor.create(recorder.getAudioSessionId()).setEnabled(true);
        }
        if (AcousticEchoCanceler.isAvailable()) {
            AcousticEchoCanceler.create(recorder.getAudioSessionId()).setEnabled(true);
        }
        recorder.startRecording();
//        MediaRecorder mr = new MediaRecorder();
//        mr.setAudioSource(MediaRecorder.AudioSource.MIC);
//        mr.setAudioChannels(1);
//        mr.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
//        mr.setAudioSamplingRate(sampleRate);
//        mr.setAudioEncodingBitRate(128000);
//        mr.start();
        try{
//            DeflaterOutputStream deflaterOutputStream = new DeflaterOutputStream(outgoing.getOutputStream());

            long totalUpload = 0;
            final int nsam = codec2.codec2_samples_per_frame();
            final short[] buf = new short[ nsam ];
//            final int nsam2 = nsam << 1;// java
//            final byte[] bytebuf = new byte[ nsam2 ];
            final int nbit = codec2.codec2_bits_per_frame();
            final byte[] bits = new byte[ nbit ];
            int quietVoice = 7;
            while(status) {
                if(!mute){
                    //reading data from MIC into buffer
                    recorder.read(buf, 0, nsam);
                    //converting to short
//                    ByteBuffer.wrap( bytebuf, 0, nsam2 ).order( ByteOrder.LITTLE_ENDIAN ).asShortBuffer().get( buf, 0, nsam );
                    int foundPeak=searchThreshold(buf,threshold);
                    if (foundPeak>-1){ //found signal
                        //send signal
                        codec2.codec2_encode(bits,buf);
                        outgoing.getOutputStream().write(bits);
                        Log.d("ANONYMOUSMESSENGER","sent size: " + bits.length);
                        //send length to ui
                        totalUpload+=bits.length;
                        Intent gcm_rec = new Intent("call_action");
                        gcm_rec.putExtra("action","upload");
                        gcm_rec.putExtra("upload",totalUpload);
                        LocalBroadcastManager.getInstance(app).sendBroadcast(gcm_rec);
                        //send 7 segments of quiet after actual voice was sent to finish off sentences better
                        quietVoice = 7;
                    }else if (quietVoice>0){//we've just detected signal recently so let's try to smooth out the ending by adding segments of low volume speech
                        //send signal
                        codec2.codec2_encode(bits,buf);
                        outgoing.getOutputStream().write(bits);
                        Log.d("ANONYMOUSMESSENGER","sent size: " + bits.length);
                        //send length to ui
                        totalUpload+=bits.length;
                        Intent gcm_rec = new Intent("call_action");
                        gcm_rec.putExtra("action","upload");
                        gcm_rec.putExtra("upload",totalUpload);
                        LocalBroadcastManager.getInstance(app).sendBroadcast(gcm_rec);
                        quietVoice-=1;
                    }else{//quietness
                        //send 0 to ui
                        Intent gcm_rec = new Intent("call_action");
                        gcm_rec.putExtra("action","upload");
                        gcm_rec.putExtra("upload", (long)0);
                        LocalBroadcastManager.getInstance(app).sendBroadcast(gcm_rec);
                    }
                }
            }
        }catch (Exception e){
            Log.e("sendData:","ERROR");
            e.printStackTrace();
            recorder.release();
            stopCall(true);
        }
    }

    public void startListening(){
        try {
            at.play();
//            final Jcodec2 codec2 = new Jcodec2( Jcodec2.CODEC2_MODE_1300 );
            final int nsam = codec2.codec2_samples_per_frame();
            final short[] buf = new short[ nsam ];
            final int nbit = codec2.codec2_bits_per_frame();
            final byte[] bits = new byte[ nbit ];

            listen(codec2,bits,buf);
            new Thread(this::startTiming).start();
            while(status){
                listen(codec2,bits,buf);
            }
        } catch (Exception e) {
            Log.e("receiveData:","receiveData: ERROR");
            e.printStackTrace();
            stopCall(true);
        }
    }

    private void listen(Jcodec2 codec2, byte[] bits, short[] buf) throws IOException {
//        byte[] buf = new byte[minBufSize];
        incoming.getInputStream().read(bits, 0, bits.length);
        codec2.codec2_decode( buf, bits );
        toSpeaker(buf);
//        toSpeaker(buf);
        Log.d("ANONYMOUSMESSENGER","received size: " +bits.length);
//        //send length to ui
//        Intent gcm_rec = new Intent("call_action");
//        gcm_rec.putExtra("action","download");
//        gcm_rec.putExtra("download",bits.length);
//        LocalBroadcastManager.getInstance(app).sendBroadcast(gcm_rec);
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

    public void toSpeaker(short[] soundBytes) {
        try {
            at.write(soundBytes,0,soundBytes.length);
            at.flush();
            at.play();
        } catch (Exception ignored) {
            Log.d("ANONYMOUSMESSENGER","Not working in speakers...");
            //e.printStackTrace();
        }
    }

    public static int searchThreshold(short[]arr,short thr){
        int peakIndex;
        int arrLen=arr.length;
        for (peakIndex=0;peakIndex<arrLen;peakIndex++){
            if ((arr[peakIndex]>=thr) || (arr[peakIndex]<=-thr)){
                //se supera la soglia, esci e ritorna peakindex-mezzo kernel.

                return peakIndex;
            }
        }
        return -1; //not found
    }


    //experimental
//    public boolean isCallActive(Context context){
//        AudioManager manager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
//        return manager.getMode() == AudioManager.MODE_IN_CALL;
//    }

    public static void callReceiveHandler(Socket sock, DxApplication app) throws Exception {
        Log.d("SERVER CONNECTION", "its a call");
        DataOutputStream outputStream = new DataOutputStream(sock.getOutputStream());
        DataInputStream in=new DataInputStream(sock.getInputStream());
        //todo check to see if busy or something, //seems to rely on phone state permissions
        outputStream.writeUTF("ok");
        outputStream.flush();
        String msg = in.readUTF();
        if(msg.trim().endsWith(".onion")){
            String address= msg.trim();
            //check if we want this guy calling us
            if(!DbHelper.contactExists(address,app)){
                //send hangup
                DbHelper.saveLog("REFUSED CALL BECAUSE RECEIVING caller is UNKNOWN "+address,new Date().getTime(),"SEVERE",app);
                outputStream.writeUTF("nuf");
                outputStream.flush();
                sock.close();
                return;
            }
            byte[] chunkSize = new byte[4];
            in.read(chunkSize,0,chunkSize.length);
            int casted = ByteBuffer.wrap(chunkSize).order(ByteOrder.LITTLE_ENDIAN).getInt();
            if(casted==0 || casted>200){
                return;
            }
            byte[] buffer = new byte[casted];
            in.read(buffer,0,casted);

            buffer = MessageEncryptor.decrypt(buffer,app.getEntity().getStore(),new SignalProtocolAddress(address,1));
            String address2 = new String(buffer, StandardCharsets.UTF_8);
            if(!address2.equals(address)){
                //send hangup
                DbHelper.saveLog("REFUSED CALL BECAUSE RECEIVING ADDRESS DID NOT MATCH ENCRYPTED ONE "+address,new Date().getTime(),"SEVERE",app);
                outputStream.writeUTF("nuf");
                outputStream.flush();
                sock.close();
                return;
            }
            outputStream.writeUTF("ok");
            outputStream.flush();
            msg = in.readUTF();
            sock.setKeepAlive(true);
            if(msg.equals(DxCallService.ACTION_START_OUTGOING_CALL_RESPONSE)){
                if(!app.isInCall()){
                    //send hangup
                    DbHelper.saveLog("REFUSED CALL BECAUSE RECEIVING END SENT A RESPONSE TO A CALL THAT IS NOT ONGOING "+address,new Date().getTime(),"SEVERE",app);
                    outputStream.writeUTF("nuf");
                    outputStream.flush();
                    sock.close();
                    return;
                }
                app.getCc().setIncoming(sock,address);
            }else if(msg.equals(DxCallService.ACTION_START_INCOMING_CALL)){
                // add incoming call to message log
                DbHelper.saveMessage(new QuotedUserMessage("","", address, "", DbHelper.getContactNickname(address,app), new Date().getTime(), true, app.getHostname(), false, "", "", "call"), app,address,true);
                //broadcast to message log
                Intent gcm_rec = new Intent("your_action");
                gcm_rec.putExtra("address",address.substring(0,10));
                LocalBroadcastManager.getInstance(app.getApplicationContext()).sendBroadcast(gcm_rec);
                if(app.isInCall()){
                    //send hangup
                    DbHelper.saveLog("REFUSED CALL BECAUSE WE ARE BUSY ON ANOTHER CALL "+address,new Date().getTime(),"NOTICE",app);
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
