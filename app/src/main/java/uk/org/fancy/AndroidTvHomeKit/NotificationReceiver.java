package uk.org.fancy.AndroidTvHomeKit;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class NotificationReceiver extends BroadcastReceiver {
    public static final String TAG = "HomeKit:HomeKitService";

    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "Starting HomeKitService");
        Intent serviceIntent = new Intent();
        serviceIntent.setClass(context, HomeKitService.class);
        context.startService(serviceIntent);
    }
}
