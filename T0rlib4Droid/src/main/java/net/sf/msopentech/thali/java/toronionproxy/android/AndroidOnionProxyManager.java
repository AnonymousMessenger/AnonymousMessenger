/*
Copyright (C) 2011-2014 Sublime Software Ltd

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */

/*
Copyright (c) Microsoft Open Technologies, Inc.
All Rights Reserved
Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0

THIS CODE IS PROVIDED ON AN *AS IS* BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, EITHER EXPRESS OR IMPLIED,
INCLUDING WITHOUT LIMITATION ANY IMPLIED WARRANTIES OR CONDITIONS OF TITLE, FITNESS FOR A PARTICULAR PURPOSE,
MERCHANTABLITY OR NON-INFRINGEMENT.

See the Apache 2 License for the specific language governing permissions and limitations under the License.
*/

package net.sf.msopentech.thali.java.toronionproxy.android;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import net.sf.msopentech.thali.java.toronionproxy.OnionProxyManager;

//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

import static android.content.Context.CONNECTIVITY_SERVICE;
import static android.net.ConnectivityManager.EXTRA_NO_CONNECTIVITY;

public class AndroidOnionProxyManager extends OnionProxyManager {
//    private static final Logger LOG = LoggerFactory.getLogger(AndroidOnionProxyManager.class);

//    private volatile BroadcastReceiver networkStateReceiver;
//    private final Context context;

    public AndroidOnionProxyManager(Context context, String workingSubDirectoryName) {
        super(new AndroidOnionProxyContext(context, workingSubDirectoryName));
//        this.context = context;
    }



    @Override
    public boolean installAndStartTorOp() throws IOException, InterruptedException {
        if (super.installAndStartTorOp()) {
            // Register to receive network status events
//            networkStateReceiver = new NetworkStateReceiver();
//            IntentFilter filter = new IntentFilter(CONNECTIVITY_ACTION);
//            context.registerReceiver(networkStateReceiver, filter);
            return true;
        }
        return false;
    }

    @Override
    public void stop() throws IOException {
        super.stop();
    }

    @SuppressLint("NewApi")
    protected boolean setExecutable(File f) {
        return f.setExecutable(true, true);
    }

    private class NetworkStateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context ctx, Intent i) {
            try {
                if(!isRunning()) return;
            } catch (IOException e) {
                System.out.println("Did someone call before Tor was ready?"+ e);
                return;
            }
            boolean online = !i.getBooleanExtra(EXTRA_NO_CONNECTIVITY, false);
            if(online) {
                // Some devices fail to set EXTRA_NO_CONNECTIVITY, double check
                Object o = ctx.getSystemService(CONNECTIVITY_SERVICE);
                ConnectivityManager cm = (ConnectivityManager) o;
                NetworkInfo net = cm.getActiveNetworkInfo();
                if(net == null || !net.isConnected()) online = false;
            }
            System.out.println("Online: " + online);
            try {
                enableNetwork(online);
            } catch(Exception e) {
                System.out.println(e.toString()+e);
            }
        }
    }
}