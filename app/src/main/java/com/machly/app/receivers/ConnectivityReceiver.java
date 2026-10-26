package com.machly.app.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.machly.app.sync.SyncManager;
import com.machly.app.utils.NetworkUtils;

public class ConnectivityReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (NetworkUtils.isOnline(context)) {
            Log.d("MachlySync", "Network connected, starting sync...");
            SyncManager.getInstance().fetchAndSaveUsers(context, new SyncManager.SyncCallback() {
                @Override
                public void onSuccess() {
                    // Use a Handler or runOnUiThread if needed to show Toast from background thread context if SyncManager runs on bg
                    // But BroadcastReceiver runs on main thread, SyncManager callbacks might be on main thread depending on implementation
                    // Here we just log
                    Log.d("MachlySync", "Sync successful after reconnect");
                }

                @Override
                public void onError(Throwable t) {
                    Log.e("MachlySync", "Sync failed after reconnect", t);
                }
            });
        }
    }
}
