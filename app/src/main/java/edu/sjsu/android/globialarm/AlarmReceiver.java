package edu.sjsu.android.globialarm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import java.util.Calendar;

public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String alarmId = intent.getStringExtra("ALARM_ID");

        // Start alarm dismiss activity
        Intent fullScreenIntent = new Intent(context, AlarmDismissActivity.class);
        fullScreenIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(fullScreenIntent);

        // Start alarm service for sound
        Intent serviceIntent = new Intent(context, AlarmService.class);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }

        // Handle recurring alarms
        if (alarmId != null) {
            rescheduleAlarmIfNeeded(context, alarmId);
        }
    }

    private void rescheduleAlarmIfNeeded(Context context, String alarmId) {
        SharedPreferences preferences = context.getSharedPreferences("AlarmPrefs", Context.MODE_PRIVATE);
        String recurrence = preferences.getString(alarmId + "_recurrence", "One-time");

        if (!recurrence.equals("One-time")) {
            long lastTriggerTime = preferences.getLong(alarmId + "_time", 0);
            String timezone = preferences.getString(alarmId + "_timezone", "");

            Calendar nextAlarm = Calendar.getInstance();
            nextAlarm.setTimeInMillis(lastTriggerTime);

            // Calculate next alarm time based on recurrence
            switch (recurrence) {
                case "Daily":
                    nextAlarm.add(Calendar.DAY_OF_YEAR, 1);
                    break;
                case "Weekly":
                    nextAlarm.add(Calendar.WEEK_OF_YEAR, 1);
                    break;
                case "Monthly":
                    nextAlarm.add(Calendar.MONTH, 1);
                    break;
                case "Yearly":
                    nextAlarm.add(Calendar.YEAR, 1);
                    break;
            }

            // Update stored time
            SharedPreferences.Editor editor = preferences.edit();
            editor.putLong(alarmId + "_time", nextAlarm.getTimeInMillis());
            editor.apply();

            // Schedule next alarm
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(context, AlarmReceiver.class);
            intent.putExtra("ALARM_ID", alarmId);

            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    alarmId.hashCode(),
                    intent,
                    PendingIntent.FLAG_IMMUTABLE
            );

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        nextAlarm.getTimeInMillis(),
                        pendingIntent
                );
            }
        }
    }
}