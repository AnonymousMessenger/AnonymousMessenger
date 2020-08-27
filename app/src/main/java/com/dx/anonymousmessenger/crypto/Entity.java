package com.dx.anonymousmessenger.crypto;

import com.dx.anonymousmessenger.DxApplication;

import org.whispersystems.libsignal.IdentityKey;
import org.whispersystems.libsignal.IdentityKeyPair;
import org.whispersystems.libsignal.InvalidKeyException;
import org.whispersystems.libsignal.SignalProtocolAddress;
import org.whispersystems.libsignal.ecc.ECKeyPair;
import org.whispersystems.libsignal.ecc.ECPublicKey;
import org.whispersystems.libsignal.ratchet.BobSignalProtocolParameters;
import org.whispersystems.libsignal.ratchet.RatchetingSession;
import org.whispersystems.libsignal.state.SessionState;
import org.whispersystems.libsignal.state.SignalProtocolStore;
import org.whispersystems.libsignal.util.KeyHelper;
import org.whispersystems.libsignal.util.guava.Optional;

public class Entity {

    private final SignalProtocolStore store;
//    private PreKeyBundle preKey;
//    private final int preKeyId;
    private final SignalProtocolAddress address;
//    private ECPublicKey ephemeralPublicKey;
//    private ECPrivateKey ephemeralPrivateKey;
//    private ECKeyPair ephemeralKey;
//    private ECKeyPair baseKey;
//    private ECKeyPair signedPreKeyPair;
//    private ECKeyPair preKeyPair;


    public Entity(DxApplication app) {
        this.address = new SignalProtocolAddress(app.getHostname(), 1);

//        this.store = new InMemorySignalProtocolStore(
//                KeyHelper.generateIdentityKeyPair(),
//                KeyHelper.generateRegistrationId(false)
//        );
        this.store = new DxSignalKeyStore(
                KeyHelper.generateIdentityKeyPair(),
                KeyHelper.generateRegistrationId(false),
                app);

//        IdentityKeyPair identityKeyPair = store.getIdentityKeyPair();
//        int registrationId = store.getLocalRegistrationId();

//        ECKeyPair preKeyPair = Curve.generateKeyPair();
//        ECKeyPair signedPreKeyPair = Curve.generateKeyPair();
//        int deviceId = 1;
//        long timestamp = System.currentTimeMillis();

//        byte[] signedPreKeySignature = Curve.calculateSignature(
//                identityKeyPair.getPrivateKey(),
//                signedPreKeyPair.getPublicKey().serialize());

//        IdentityKey identityKey = identityKeyPair.getPublicKey();
//        ECPublicKey preKeyPublic = preKeyPair.getPublicKey();
//        ECPublicKey signedPreKeyPublic = signedPreKeyPair.getPublicKey();
//
//        this.preKeyPair = preKeyPair;
//        this.signedPreKeyPair = signedPreKeyPair;
//
//        this.preKey = new PreKeyBundle(
//                registrationId,
//                deviceId,
//                preKeyId,
//                preKeyPublic,
//                signedPreKeyId,
//                signedPreKeyPublic,
//                signedPreKeySignature,
//                identityKey);
//
//        PreKeyRecord preKeyRecord = new PreKeyRecord(preKey.getPreKeyId(), preKeyPair);
//        SignedPreKeyRecord signedPreKeyRecord = new SignedPreKeyRecord(
//                signedPreKeyId, timestamp, signedPreKeyPair, signedPreKeySignature);
//
//        store.storePreKey(preKeyId, preKeyRecord);
//        store.storeSignedPreKey(signedPreKeyId, signedPreKeyRecord);

//        this.ephemeralPublicKey  = Curve.decodePoint(preKeyPublic.serialize(), 0);
//        this.ephemeralPrivateKey = Curve.decodePrivatePoint(preKeyPair.getPrivateKey().serialize());
//
//        this.ephemeralKey = new ECKeyPair(ephemeralPublicKey, ephemeralPrivateKey);
//        this.baseKey = ephemeralKey;
    }

    public SignalProtocolStore getStore() {
        return store;
    }

    public SessionState ratchetSession(IdentityKey aliceIdentityPublicKey,ECPublicKey aliceBasePublicKey) throws InvalidKeyException{

        IdentityKeyPair identityKeyPair = store.getIdentityKeyPair();


        BobSignalProtocolParameters parameters = BobSignalProtocolParameters.newBuilder()
                .setOurIdentityKey(identityKeyPair)
//                .setOurSignedPreKey(this.signedPreKeyPair)
//                .setOurRatchetKey(this.getEphemeralKey())
                .setOurOneTimePreKey(Optional.<ECKeyPair>absent())
                .setTheirIdentityKey(aliceIdentityPublicKey)
                .setTheirBaseKey(aliceBasePublicKey)
                .create();

        SessionState session = new SessionState();

        RatchetingSession.initializeSession(session, parameters);


        return session;

//        this.preKey = new PreKeyBundle(
//                registrationId,
//                deviceId,
//                preKeyId,
//                preKeyPublic,
//                signedPreKeyId,
//                signedPreKeyPublic,
//                signedPreKeySignature,
//                identityKey);
//
//        PreKeyRecord preKeyRecord = new PreKeyRecord(preKey.getPreKeyId(), myPreKeyPair);
//        SignedPreKeyRecord signedPreKeyRecord = new SignedPreKeyRecord(
//                signedPreKeyId, timestamp, mySignedPreKeyPair, signedPreKeySignature);
//
//        store.storePreKey(preKeyId, preKeyRecord);
//        store.storeSignedPreKey(signedPreKeyId, signedPreKeyRecord);

    }

//    public PreKeyBundle getPreKey() {
//        return preKey;
//    }

    public SignalProtocolAddress getAddress() {
        return address;
    }

//    public ECPublicKey getEphemeralPublicKey() {
//        return ephemeralPublicKey;
//    }
//
//    public void setEphemeralPublicKey(ECPublicKey ephemeralPublicKey) {
//        this.ephemeralPublicKey = ephemeralPublicKey;
//    }
//
//    public ECPrivateKey getEphemeralPrivateKey() {
//        return ephemeralPrivateKey;
//    }
//
//    public void setEphemeralPrivateKey(ECPrivateKey ephemeralPrivateKey) {
//        this.ephemeralPrivateKey = ephemeralPrivateKey;
//    }
//
//    public ECKeyPair getEphemeralKey() {
//        return ephemeralKey;
//    }
//
//    public void setEphemeralKey(ECKeyPair ephemeralKey) {
//        this.ephemeralKey = ephemeralKey;
//    }
//
//    public ECKeyPair getBaseKey() {
//        return baseKey;
//    }
//
//    public void setBaseKey(ECKeyPair baseKey) {
//        this.baseKey = baseKey;
//    }
//
//    public ECKeyPair getSignedPreKeyPair() {
//        return signedPreKeyPair;
//    }
//
//    public void setSignedPreKeyPair(ECKeyPair signedPreKeyPair) {
//        this.signedPreKeyPair = signedPreKeyPair;
//    }
}