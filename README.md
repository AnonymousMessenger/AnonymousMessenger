# AnonymousMessenger
Official repository:    https://git.anonymousmessenger.ly/dx/AnonymousMessenger
Official website:       https://www.anonymousmessenger.ly/

## What is it
A peer to peer private anonymous and secure messenger that works over tor. It's also free and open source software which gives users the freedom of changing it and redistributing it under the terms of the GNU General Public License v3.

## How it works
Anonymous Messenger utilizes tor for it's anonymity network and data transport security when using the onion v3 protocol and the ability to run onion services on any device, also it uses the signal protocol to encrypt all data before sending it over the tor network directly to the intended receiver, which means we get two layers of end-to-end encryption without having to use any server or service.
Anonymous Messenger also encrypts data stored on the user's device with the user's password using SQLcipher for the database and 128 bit AES for files.
Anonymous Messenger uses it's own protocol for communication which is written for security and simplicity.

## How to build from source
Anonymous Messenger can be built as an android project (eg. using Android Studio).
we recommend you change the applicationId if you want to install multiple versions on the same device due to signature problems.
### How to build binary dependancies
Anonymous Messenger depends on tor and obfs4proxy and they must be included for your device's architecture in the jniLibs directory 'app/src/main/jniLibs/[ARCH]' and they need to be named libtor.ARCH.so and obfs4proxy.ARCH.so

We have forked the tor-android repository to provide a script that builds tor for our supported architectures (armeabi-v7a,arm64-v8a,x86,x86_64) visit: https://git.anonymousmessenger.ly/dx/tor-android

We will create a new repository with an easy script to build obfs4proxy soon.


