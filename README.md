# AnonymousMessenger
Official repository:    https://git.anonymousmessenger.ly/dx/AnonymousMessenger

Official website:       https://www.anonymousmessenger.ly/

Translate:              https://www.transifex.com/liberty-for-all/anonymous-messenger/

## What is it
A peer to peer private anonymous and secure messenger that works over Tor. It's also free and open source software which gives users the freedom of changing it and redistributing it under the terms of the GNU General Public License v3.

This project was started in July 2020, by Sofian Mahmoud Benissa (software developer also known as Drxtreme or DX) after a series of meetings with Alton James Steele (project funder) where the idea of the project was presented.

### Features
    * Double triple Diffie-Hellman end to end encryption
    * Completely peer to peer using hidden services
    * Cryptographic Identity Verification
    * Excellent Network Security
    * Voice Messages
    * Live Voice Calls over tor (alpha feature)
    * Text Messages
    * Metadata stripped media messages
    * Raw file sending of any size (100 GB+)
    * Both peers have to add each others onion addresses to be able to communicate
    * Disappearing messages by default
    * Encrypted file storage on Android
    * Screen security

## How it works

### Network and encryption
Anonymous Messenger utilizes Tor for it's anonymity network and data transport security using the onion v3 protocol and the ability to run onion services on any device and connect directly and anonymously without having to set up any servers, also it uses the signal protocol to encrypt all data before sending it over the Tor network directly to the intended receiver, which means we get two layers of end-to-end encryption without using any server or service.

### Transport
Instead of using http Anonymous Messenger uses it's own transport protocol which is written for anonymity and simplicity.
For example when we send a message we encrypt it using the Signal protocol then we establish an onionV3 encrypted connection and send the encrypted message over the already encrypted connection.

### Storage
Anonymous Messenger encrypts data stored on the user's device with the user's password using SQLcipher for the database and encrypts all files with AES/GCM/NoPadding cipher.



## How to build from source
Anonymous Messenger can be built as an android project (eg. using Android Studio or Gradle).
We recommend you change the applicationId if you want to install multiple versions on the same device.

### How to build binary dependancies
Anonymous Messenger depends on Tor and obfs4proxy and they must be included for your device's architecture in the jniLibs directory 'app/src/main/jniLibs/[ARCH]' and they need to be named libtor.ARCH.so and obfs4proxy.ARCH.so

We have forked the tor-android repository to provide a script that builds Tor for our supported architectures (armeabi-v7a,arm64-v8a,x86,x86_64) visit: https://git.anonymousmessenger.ly/dx/tor-android

To build obfs4proxy for android we have an empty repo with a readme that contains the commands needed, visit: https://git.anonymousmessenger.ly/dx/obfs4android
