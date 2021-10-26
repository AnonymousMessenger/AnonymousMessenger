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

package net.sf.msopentech.thali.java.toronionproxy;

import android.content.Intent;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import net.sf.controller.network.NetLayerStatus;
import net.sf.controller.network.ServiceDescriptor;
import net.sf.freehaven.tor.control.EventHandler;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Logs the data we get from notifications from the Tor OP. This is really just meant for debugging.
 */
public class OnionProxyManagerEventHandler implements EventHandler {
//    private static final Logger LOG = LoggerFactory.getLogger(OnionProxyManagerEventHandler.class);
    private ServiceDescriptor hs;
    private boolean hsPublished;
    private final OnionProxyContext onionProxyContext;

    public OnionProxyManagerEventHandler(OnionProxyContext onionProxyContext) {
        this.onionProxyContext = onionProxyContext;
//        sendStatuses();
    }

    public void setHStoWatchFor(ServiceDescriptor hs, NetLayerStatus listener) {
        if (hs == this.hs && hsPublished) {
            listener.onConnect(hs);
            return;
        }
        this.hs = hs;
        hsPublished = false;
    }

    public void circuitStatus(String status, String id, List<String> path, Map<String, String> info) {
        String msg = "CircuitStatus: " + id + " " + status;
        String purpose = info.get("PURPOSE");
        if (purpose != null) msg += ", purpose: " + purpose;
        String hsState = info.get("HS_STATE");
        if (hsState != null) msg += ", state: " + hsState;
        String rendQuery = info.get("REND_QUERY");
        if (rendQuery != null) msg += ", service: " + rendQuery;
        if (!path.isEmpty()) msg += ", path: " + shortenPath(path);
//        LOG.info(msg);
//        Intent gcm_rec = new Intent("tor_status");
//        gcm_rec.putExtra("tor_status",msg);
//        LocalBroadcastManager.getInstance(onionProxyContext.ctx).sendBroadcast(gcm_rec);
    }

    @Override
    public void circuitStatus(String status, String circID, String path) {
        //LOG.info("status: " + status + ", circID: " + circID + ", path: " + path);
        try{
            Intent gcm_rec = new Intent("tor_status");
            StringBuilder output = new StringBuilder();
            for (String relay: path.split(",")){
                output.append(relay.split("~")[1]);
                output.append(" > ");
            }
            gcm_rec.putExtra("tor_status","circ: " + circID + " "+status+", (" + output + ")");
            LocalBroadcastManager.getInstance(onionProxyContext.ctx).sendBroadcast(gcm_rec);
        }catch (Exception ignored) {}
    }

    public void streamStatus(String status, String id, String target) {
        //LOG.info("status: " + status + ", id: " + id + ", target: " + target);
//        Intent gcm_rec = new Intent("tor_status");
//        gcm_rec.putExtra("tor_status","status: " + status + ", id: " + id + ", target: " + target);
//        LocalBroadcastManager.getInstance(onionProxyContext.ctx).sendBroadcast(gcm_rec);
    }

    public void orConnStatus(String status, String orName) {
        //LOG.info("OR connection: status: " + status + ", orName: " + orName);
//        Intent gcm_rec = new Intent("tor_status");
//        gcm_rec.putExtra("tor_status","OR connection: status: " + status + ", orName: " + orName);
//        LocalBroadcastManager.getInstance(onionProxyContext.ctx).sendBroadcast(gcm_rec);
    }

    public void bandwidthUsed(long read, long written) {
        Log.d("ANONYMOUSMESSENGER","bandwidthUsed: read: " + read + ", written: " + written);
        Intent gcm_rec = new Intent("tor_status");
        gcm_rec.putExtra("tor_status","bandwidthUsed: read: " + read + ", written: " + written);
        LocalBroadcastManager.getInstance(onionProxyContext.ctx).sendBroadcast(gcm_rec);
    }

    public void newDescriptors(List<String> orList) {
        Iterator<String> iterator = orList.iterator();
        StringBuilder stringBuilder = new StringBuilder();
        while (iterator.hasNext()) {
            stringBuilder.append(iterator.next());
        }
        Log.d("ANONYMOUSMESSENGER","newDescriptors: " + stringBuilder.toString());
        Intent gcm_rec = new Intent("tor_status");
        gcm_rec.putExtra("tor_status","newDescriptors: " + stringBuilder.toString());
        LocalBroadcastManager.getInstance(onionProxyContext.ctx).sendBroadcast(gcm_rec);
    }

    //fetch Exit Node
    public void message(String severity, String msg) {
        Log.d("ANONYMOUSMESSENGER",severity + "," + msg);
        Intent gcm_rec = new Intent("tor_status");
        gcm_rec.putExtra("tor_status",severity + "," + msg);
        LocalBroadcastManager.getInstance(onionProxyContext.ctx).sendBroadcast(gcm_rec);
    }

    public void unrecognized(String type, String msg) {
        Log.d("ANONYMOUSMESSENGER","unrecognized: msg: " + type + ", " + msg);
        Intent gcm_rec = new Intent("tor_status");
        gcm_rec.putExtra("tor_status","unrecognized: msg: " + type + ", " + msg);
        LocalBroadcastManager.getInstance(onionProxyContext.ctx).sendBroadcast(gcm_rec);
    }

    private String shortenPath(List<String> path) {
        StringBuilder s = new StringBuilder();
        for (String id : path) {
            if (s.length() > 0) s.append(',');
            s.append(id.substring(1, 7));
        }
        return s.toString();
    }

}
