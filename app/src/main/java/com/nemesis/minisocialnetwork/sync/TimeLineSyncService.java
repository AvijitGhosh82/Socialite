package com.nemesis.minisocialnetwork.sync;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class TimeLineSyncService extends Service {
    private static final Object sSyncAdapterLock = new Object();
    private static TimeLineSyncAdapter sTimeLineSyncAdapter = null;

    @Override
    public void onCreate() {
        Log.d("SunshineSyncService", "onCreate - SunshineSyncService");
        synchronized (sSyncAdapterLock) {
            if (sTimeLineSyncAdapter == null) {
                sTimeLineSyncAdapter = new TimeLineSyncAdapter(getApplicationContext(), true);
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return sTimeLineSyncAdapter.getSyncAdapterBinder();
    }
}