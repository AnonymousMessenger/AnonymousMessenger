package com.dx.anonymousmessenger.crypto;

import org.whispersystems.libsignal.SessionCipher;
import org.whispersystems.libsignal.SignalProtocolAddress;
import org.whispersystems.libsignal.state.SignalProtocolStore;

public class StreamSessionCipher extends SessionCipher {
    public StreamSessionCipher(SignalProtocolStore store, SignalProtocolAddress remoteAddress) {
        super(store, remoteAddress);
    }
}
