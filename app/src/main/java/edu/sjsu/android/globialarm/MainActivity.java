package edu.sjsu.android.globialarm;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

public class MainActivity extends AppCompatActivity {
    private ListView alarmList;
    private ArrayAdapter<String> adapter;
    private ArrayList<String> alarms;
    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        preferences = getSharedPreferences("AlarmPrefs", MODE_PRIVATE);
        alarms = new ArrayList<>();

        initializeViews();
        setupAdapter();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAlarms();
    }

    private void initializeViews() {
        alarmList = findViewById(R.id.alarmList);
        FloatingActionButton addButton = findViewById(R.id.addAlarmButton);
        addButton.setOnClickListener(v -> startActivity(new Intent(this, AlarmSetterActivity.class)));
    }

    private void setupAdapter() {
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, alarms);
        alarmList.setAdapter(adapter);

        // Click listener for editing alarms
        alarmList.setOnItemClickListener((parent, view, position, id) -> {
            Map<String, ?> allPrefs = preferences.getAll();
            List<AlarmData> sortedAlarms = new ArrayList<>();
            List<String> sortedAlarmIds = new ArrayList<>();

            // Collect all alarms with their IDs
            for (String key : allPrefs.keySet()) {
                if (key.endsWith("_name")) {
                    String alarmId = key.replace("_name", "");
                    String name = preferences.getString(key, "");
                    long time = preferences.getLong(alarmId + "_time", 0);
                    String timezone = preferences.getString(alarmId + "_timezone", "");
                    String recurrence = preferences.getString(alarmId + "_recurrence", "One-time");

                    sortedAlarms.add(new AlarmData(name, time, timezone, recurrence));
                    sortedAlarmIds.add(alarmId);
                }
            }

            // Sort both lists using the same comparator as in loadAlarms()
            Collections.sort(sortedAlarms, (a1, a2) -> Long.compare(
                    convertToUTC(a1.time, a1.timezone),
                    convertToUTC(a2.time, a2.timezone)
            ));

            if (position < sortedAlarms.size()) {
                AlarmData selectedAlarm = sortedAlarms.get(position);
                String selectedAlarmId = sortedAlarmIds.get(
                        sortedAlarms.indexOf(selectedAlarm)
                );

                Intent editIntent = new Intent(this, AlarmSetterActivity.class);
                editIntent.putExtra("EDIT_MODE", true);
                editIntent.putExtra("ALARM_ID", selectedAlarmId);
                editIntent.putExtra("ALARM_NAME", selectedAlarm.name);
                editIntent.putExtra("ALARM_TIME", selectedAlarm.time);
                editIntent.putExtra("ALARM_TIMEZONE", selectedAlarm.timezone);
                editIntent.putExtra("ALARM_RECURRENCE", selectedAlarm.recurrence);
                startActivity(editIntent);
            }
        });

        // Long click listener for deleting alarms
        alarmList.setOnItemLongClickListener((parent, view, position, id) -> {
            new AlertDialog.Builder(this)
                    .setTitle("Delete Alarm")
                    .setMessage("Are you sure?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        deleteAlarm(position);
                        cancelScheduledAlarm(position);
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            return true;
        });
    }

    private void cancelScheduledAlarm(int position) {
        Map<String, ?> allPrefs = preferences.getAll();
        List<AlarmData> sortedAlarms = new ArrayList<>();
        List<String> sortedAlarmIds = new ArrayList<>();

        for (String key : allPrefs.keySet()) {
            if (key.endsWith("_name")) {
                String alarmId = key.replace("_name", "");
                String name = preferences.getString(key, "");
                long time = preferences.getLong(alarmId + "_time", 0);
                String timezone = preferences.getString(alarmId + "_timezone", "");
                String recurrence = preferences.getString(alarmId + "_recurrence", "One-time");

                sortedAlarms.add(new AlarmData(name, time, timezone, recurrence));
                sortedAlarmIds.add(alarmId);
            }
        }

        Collections.sort(sortedAlarms, (a1, a2) -> Long.compare(
                convertToUTC(a1.time, a1.timezone),
                convertToUTC(a2.time, a2.timezone)
        ));

        if (position < sortedAlarms.size()) {
            String alarmId = sortedAlarmIds.get(sortedAlarms.indexOf(sortedAlarms.get(position)));
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(this, AlarmReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    this,
                    alarmId.hashCode(),
                    intent,
                    PendingIntent.FLAG_IMMUTABLE
            );
            alarmManager.cancel(pendingIntent);
        }
    }

    private void deleteAlarm(int position) {
        Map<String, ?> allPrefs = preferences.getAll();
        List<AlarmData> sortedAlarms = new ArrayList<>();
        List<String> sortedAlarmIds = new ArrayList<>();

        for (String key : allPrefs.keySet()) {
            if (key.endsWith("_name")) {
                String alarmId = key.replace("_name", "");
                String name = preferences.getString(key, "");
                long time = preferences.getLong(alarmId + "_time", 0);
                String timezone = preferences.getString(alarmId + "_timezone", "");
                String recurrence = preferences.getString(alarmId + "_recurrence", "One-time");

                sortedAlarms.add(new AlarmData(name, time, timezone, recurrence));
                sortedAlarmIds.add(alarmId);
            }
        }

        Collections.sort(sortedAlarms, (a1, a2) -> Long.compare(
                convertToUTC(a1.time, a1.timezone),
                convertToUTC(a2.time, a2.timezone)
        ));

        if (position < sortedAlarms.size()) {
            String alarmId = sortedAlarmIds.get(sortedAlarms.indexOf(sortedAlarms.get(position)));
            SharedPreferences.Editor editor = preferences.edit();
            editor.remove(alarmId + "_name");
            editor.remove(alarmId + "_time");
            editor.remove(alarmId + "_timezone");
            editor.remove(alarmId + "_recurrence");
            editor.apply();
            loadAlarms();
        }
    }

    private void loadAlarms() {
        alarms.clear();
        Map<String, ?> allPrefs = preferences.getAll();
        List<AlarmData> alarmList = new ArrayList<>();

        for (String key : allPrefs.keySet()) {
            if (key.endsWith("_name")) {
                String alarmId = key.replace("_name", "");
                String name = preferences.getString(key, "");
                long utcTime = preferences.getLong(alarmId + "_time", 0);
                String timezone = preferences.getString(alarmId + "_timezone", "");
                String recurrence = preferences.getString(alarmId + "_recurrence", "One-time");

                // Convert UTC time to alarm's timezone
                TimeZone targetTimezone = TimeZone.getTimeZone(timezone);
                long localTime = utcTime - targetTimezone.getOffset(utcTime);

                alarmList.add(new AlarmData(name, localTime, timezone, recurrence));
            }
        }

        // Sort alarms by their UTC time
        Collections.sort(alarmList, (a1, a2) -> Long.compare(
                convertToUTC(a1.time, a1.timezone),
                convertToUTC(a2.time, a2.timezone)
        ));

        for (AlarmData alarm : alarmList) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
            sdf.setTimeZone(TimeZone.getTimeZone(alarm.timezone));
            alarms.add(String.format("%s - %s (%s) [%s]",
                    alarm.name,
                    sdf.format(new Date(alarm.time)),
                    alarm.timezone,
                    alarm.recurrence));
        }
        adapter.notifyDataSetChanged();
    }
    private long convertToUTC(long timeInMillis, String timezone) {
        TimeZone tz = TimeZone.getTimeZone(timezone);
        return timeInMillis + tz.getOffset(timeInMillis);
    }

    private static class AlarmData {
        String name;
        long time;
        String timezone;
        String recurrence;

        AlarmData(String name, long time, String timezone, String recurrence) {
            this.name = name;
            this.time = time;
            this.timezone = timezone;
            this.recurrence = recurrence;
        }
    }
}