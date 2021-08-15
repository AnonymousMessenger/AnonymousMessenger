package com.dx.anonymousmessenger.messages;

import com.dx.anonymousmessenger.crypto.AddressedEncryptedMessage;
import com.dx.anonymousmessenger.crypto.AddressedKeyExchangeMessage;
import com.dx.anonymousmessenger.crypto.KeyExchangeMessage;
import com.dx.anonymousmessenger.crypto.SessionBuilder;

import org.whispersystems.libsignal.DuplicateMessageException;
import org.whispersystems.libsignal.InvalidKeyException;
import org.whispersystems.libsignal.InvalidMessageException;
import org.whispersystems.libsignal.LegacyMessageException;
import org.whispersystems.libsignal.NoSessionException;
import org.whispersystems.libsignal.SessionCipher;
import org.whispersystems.libsignal.SignalProtocolAddress;
import org.whispersystems.libsignal.StaleKeyExchangeException;
import org.whispersystems.libsignal.UntrustedIdentityException;
import org.whispersystems.libsignal.protocol.SignalMessage;
import org.whispersystems.libsignal.state.SignalProtocolStore;

import java.nio.charset.StandardCharsets;

public class MessageEncryptor {

    public static KeyExchangeMessage getKeyExchangeMessage(SignalProtocolStore store, SignalProtocolAddress address){
        SessionBuilder a2b = new SessionBuilder(store, address);
        return a2b.process();
    }

    public static KeyExchangeMessage getKeyExchangeMessage(SignalProtocolStore store, SignalProtocolAddress address, KeyExchangeMessage kem) throws UntrustedIdentityException, InvalidKeyException, StaleKeyExchangeException {
        SessionBuilder a2b = new SessionBuilder(store, address);
        return a2b.process(kem);
    }

    public static AddressedKeyExchangeMessage processKeyExchangeMessage(AddressedKeyExchangeMessage akem, SignalProtocolStore store) throws Exception, StaleKeyExchangeException {
        KeyExchangeMessage kem = new SessionBuilder(store, new SignalProtocolAddress(akem.getAddress(),1)).process(akem.getKem());
        return new AddressedKeyExchangeMessage(kem,akem.getAddress(),true);
    }

    //encrypt for stream
    public static byte[] encryptStream(byte[] msg, SignalProtocolStore store, SignalProtocolAddress address) throws UntrustedIdentityException {
        SessionCipher asc = new SessionCipher(store, address);
        
        return asc.encrypt(msg).serialize();
    }

    //encrypt for bytes
    public static byte[] encrypt(byte[] msg, SignalProtocolStore store, SignalProtocolAddress address) throws UntrustedIdentityException {
        SessionCipher asc = new SessionCipher(store, address);
        return asc.encrypt(msg).serialize();
    }

    //encrypt for strings (the original)
    public static byte[] encrypt(String msg, SignalProtocolStore store, SignalProtocolAddress address) throws UntrustedIdentityException {
        SessionCipher asc = new SessionCipher(store, address);
        return asc.encrypt(msg.getBytes(StandardCharsets.UTF_8)).serialize();
    }

    //decrypt for bytes
    public static byte[] decrypt(byte[] msg, SignalProtocolStore store, SignalProtocolAddress address) throws LegacyMessageException, InvalidMessageException, DuplicateMessageException, NoSessionException, UntrustedIdentityException {
        SessionCipher asc = new SessionCipher(store, address);
        return asc.decrypt(new SignalMessage(msg));
    }

    //decrypt for strings
    public static String decrypt(AddressedEncryptedMessage msg, SignalProtocolStore store, SignalProtocolAddress address) throws LegacyMessageException, InvalidMessageException, DuplicateMessageException, NoSessionException, UntrustedIdentityException {
        SessionCipher asc = new SessionCipher(store, address);
        return new String(asc.decrypt(new SignalMessage(msg.getMsg())), StandardCharsets.UTF_8);
    }
}
