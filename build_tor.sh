
git clone https://git.anonymousmessenger.ly/dx/tor-android.git


docker run -v tor-android/:/tor-android -v Android:/Android --network=host -it debian /bin/bash

#make sure ndk directory exists
export ANDROID_NDK_HOME=/Android/Sdk/ndk/21.3.6528147
export ANDROID_HOME=/Android

apt-get update
#apt-get install openjdk-8-jdk -y
apt-get install autotools-dev libsystemd-dev automake autogen autoconf libtool gettext-base autopoint git make pkg-config systemd build-essential automake libevent-dev libssl-dev zlib1g-dev -y
apt-get install systemd* -y


cd /tor-android

./tor-droid-make.sh fetch

cd external/libevent
git checkout release-2.1.11-stable #(or latest stable release)
cd ../openssl
git checkout OpenSSL_1_1_1d #(or latest stable release)
cd ../tor
git checkout tor-0.4.1.6 #(or latest stable release)
cd ../xz
git checkout v5.0.1 #(or latest stable release)
cd ../..

./tor-droid-make.sh build
export APP_ABI=x86
unset NDK_PLATFORM_LEVEL
unset PIEFLAGS
make -C external clean tor
./tor-droid-make.sh build

#rename x86
cd external/lib/x86
mv libtor.so libtor.x86.so
#rename x86_64
cd ../x86_64
mv libtor.so libtor.x86_64.so
#rename arm
cd ../armeabi-v7a
mv libtor.so libtor.arm.so
#rename arm64
cd ../arm64-v8a
mv libtor.so libtor.arm64.so

cd ../../..

echo "the .so's are in external/lib/x86 external/lib/x86_64 external/lib/armeabi-v7a external/lib/arm64-v8a-v7a "







