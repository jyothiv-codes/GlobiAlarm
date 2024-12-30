package edu.sjsu.android.globialarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // Start the AlarmService for sound playback
        Intent serviceIntent = new Intent(context, AlarmService.class);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }

        // Start the dismiss activity
        Intent fullScreenIntent = new Intent(context, AlarmDismissActivity.class);
        fullScreenIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(fullScreenIntent);
    }
}