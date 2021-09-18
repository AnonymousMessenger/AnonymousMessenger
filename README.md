# AnonymousMessenger
Official repository:    https://git.anonymousmessenger.ly/dx/AnonymousMessenger
Official website:       https://www.anonymousmessenger.ly/

## What is it
A peer to peer private anonymous and secure messenger that works over Tor. It's also free and open source software which gives users the freedom of changing it and redistributing it under the terms of the GNU General Public License v3.

## How it works
Anonymous Messenger utilizes Tor for it's anonymity network and data transport security when using the onion v3 protocol and the ability to run onion services on any device, also it uses the signal protocol to encrypt all data before sending it over the Tor network directly to the intended receiver, which means we get two layers of end-to-end encryption without having to use any server or service.
Anonymous Messenger also encrypts data stored on the user's device with the user's password using SQLcipher for the database and 128 bit AES for files.
Instead of using http Anonymous Messenger uses it's own transport protocol which is written for anonymity and simplicity.
For example when we send a message we encrypt it using the Signal protocol then we establish an onionV3 encrypted connection and send it the encrypted message over the encrypted connection.

## How to build from source
Anonymous Messenger can be built as an android project (eg. using Android Studio or Gradle).
We recommend you change the applicationId if you want to install multiple versions on the same device due to signature problems.

### How to build binary dependancies
Anonymous Messenger depends on Tor and obfs4proxy and they must be included for your device's architecture in the jniLibs directory 'app/src/main/jniLibs/[ARCH]' and they need to be named libtor.ARCH.so and obfs4proxy.ARCH.so

We have forked the tor-android repository to provide a script that builds Tor for our supported architectures (armeabi-v7a,arm64-v8a,x86,x86_64) visit: https://git.anonymousmessenger.ly/dx/tor-android

To build obfs4proxy for android we have an empty repo with a readme that contains the commands needed, visit: https://git.anonymousmessenger.ly/dx/obfs4android
