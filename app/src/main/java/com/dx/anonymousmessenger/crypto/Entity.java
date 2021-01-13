package com.dx.anonymousmessenger.crypto;

import com.dx.anonymousmessenger.DxApplication;

import org.whispersystems.libsignal.IdentityKey;
import org.whispersystems.libsignal.IdentityKeyPair;
import org.whispersystems.libsignal.InvalidKeyException;
import org.whispersystems.libsignal.ecc.ECPublicKey;
import org.whispersystems.libsignal.ratchet.BobSignalProtocolParameters;
import org.whispersystems.libsignal.ratchet.RatchetingSession;
import org.whispersystems.libsignal.state.SessionState;
import org.whispersystems.libsignal.state.SignalProtocolStore;
import org.whispersystems.libsignal.util.KeyHelper;
import org.whispersystems.libsignal.util.guava.Optional;

public class Entity {

    private final SignalProtocolStore store;


    public Entity(DxApplication app) {
        this.store = new DxSignalKeyStore(
                KeyHelper.generateIdentityKeyPair(),
                KeyHelper.generateRegistrationId(false),
                app);
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
                .setOurOneTimePreKey(Optional.absent())
                .setTheirIdentityKey(aliceIdentityPublicKey)
                .setTheirBaseKey(aliceBasePublicKey)
                .create();

        SessionState session = new SessionState();

        RatchetingSession.initializeSession(session, parameters);


        return session;

    }

}