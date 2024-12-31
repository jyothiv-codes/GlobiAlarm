package edu.sjsu.android.globialarm;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Calendar;
import java.util.TimeZone;
import java.util.UUID;

public class AlarmSetterActivity extends AppCompatActivity {
    private EditText alarmName;
    private Button dateButton, timeButton, saveButton;
    private AutoCompleteTextView timezoneAutoComplete;
    private AutoCompleteTextView recurrenceAutoComplete;
    private SharedPreferences preferences;
    private Calendar selectedDateTime;
    private String selectedTimezone;
    private String editAlarmId;
    private boolean isEditMode;
    private static final String[] RECURRENCE_OPTIONS = {
            "One-time", "Daily", "Weekly", "Monthly", "Yearly"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm_setter);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (!alarmManager.canScheduleExactAlarms()) {
                Intent intent = new Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                intent.setData(android.net.Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            }
        }

        preferences = getSharedPreferences("AlarmPrefs", MODE_PRIVATE);
        selectedDateTime = Calendar.getInstance();

        // Check if we're in edit mode
        isEditMode = getIntent().getBooleanExtra("EDIT_MODE", false);
        if (isEditMode) {
            editAlarmId = getIntent().getStringExtra("ALARM_ID");
            String name = getIntent().getStringExtra("ALARM_NAME");
            long time = getIntent().getLongExtra("ALARM_TIME", 0);
            String timezone = getIntent().getStringExtra("ALARM_TIMEZONE");
            selectedDateTime.setTimeInMillis(time);
        }

        initializeViews();
        setupTimeZoneSpinner();
        setupRecurrenceDropdown();
        setupClickListeners();

        if (isEditMode) {
            loadExistingAlarmData();
        }
    }

    private void initializeViews() {
        alarmName = findViewById(R.id.alarmName);
        dateButton = findViewById(R.id.dateButton);
        timeButton = findViewById(R.id.timeButton);
        saveButton = findViewById(R.id.saveButton);
        timezoneAutoComplete = findViewById(R.id.timezoneAutoComplete);
        recurrenceAutoComplete = findViewById(R.id.recurrenceAutoComplete);

        if (isEditMode) {
            saveButton.setText("Update Alarm");
        }
    }

    private void setupTimeZoneSpinner() {
        String[] timeZones = TimeZone.getAvailableIDs();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                R.layout.dropdown_item, timeZones);
        timezoneAutoComplete.setAdapter(adapter);
        timezoneAutoComplete.setThreshold(1);
    }

    private void setupRecurrenceDropdown() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                R.layout.dropdown_item,
                RECURRENCE_OPTIONS
        );
        recurrenceAutoComplete.setAdapter(adapter);
        recurrenceAutoComplete.setText(RECURRENCE_OPTIONS[0], false); // Default to One-time
    }

    private void setupClickListeners() {
        dateButton.setOnClickListener(v -> showDatePicker());
        timeButton.setOnClickListener(v -> showTimePicker());
        saveButton.setOnClickListener(v -> saveAlarm());
    }

    private void loadExistingAlarmData() {
        String name = getIntent().getStringExtra("ALARM_NAME");
        long time = getIntent().getLongExtra("ALARM_TIME", 0);
        String timezone = getIntent().getStringExtra("ALARM_TIMEZONE");
        String recurrence = getIntent().getStringExtra("ALARM_RECURRENCE");

        alarmName.setText(name);

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(time);

        dateButton.setText(String.format("%d/%d/%d",
                cal.get(Calendar.DAY_OF_MONTH),
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.YEAR)));

        timeButton.setText(String.format("%02d:%02d",
                cal.get(Calendar.HOUR_OF_DAY),
                cal.get(Calendar.MINUTE)));

        timezoneAutoComplete.setText(timezone);

        if (recurrence != null) {
            recurrenceAutoComplete.setText(recurrence, false);
        }
    }

    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    selectedDateTime.set(year, month, dayOfMonth);
                    dateButton.setText(String.format("%d/%d/%d", dayOfMonth, month + 1, year));
                },
                selectedDateTime.get(Calendar.YEAR),
                selectedDateTime.get(Calendar.MONTH),
                selectedDateTime.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void showTimePicker() {
        TimePickerDialog timePickerDialog = new TimePickerDialog(
                this,
                (view, hourOfDay, minute) -> {
                    selectedDateTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    selectedDateTime.set(Calendar.MINUTE, minute);
                    timeButton.setText(String.format("%02d:%02d", hourOfDay, minute));
                },
                selectedDateTime.get(Calendar.HOUR_OF_DAY),
                selectedDateTime.get(Calendar.MINUTE),
                true
        );
        timePickerDialog.show();
    }

    @SuppressLint("ScheduleExactAlarm")
    private void saveAlarm() {
        String name = alarmName.getText().toString();
        selectedTimezone = timezoneAutoComplete.getText().toString();
        String recurrence = recurrenceAutoComplete.getText().toString();

        if (name.isEmpty()) {
            Toast.makeText(this, "Please enter alarm name", Toast.LENGTH_SHORT).show();
            return;
        }

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            Toast.makeText(this, "Please allow exact alarms permission", Toast.LENGTH_LONG).show();
            return;
        }

        SharedPreferences.Editor editor = preferences.edit();
        String alarmId;

        if (isEditMode) {
            alarmId = editAlarmId;
            // Cancel existing alarm
            Intent intent = new Intent(this, AlarmReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    this,
                    alarmId.hashCode(),
                    intent,
                    PendingIntent.FLAG_IMMUTABLE
            );
            alarmManager.cancel(pendingIntent);
        } else {
            alarmId = UUID.randomUUID().toString();
        }

        // Save alarm data
        editor.putString(alarmId + "_name", name);
        editor.putLong(alarmId + "_time", selectedDateTime.getTimeInMillis());
        editor.putString(alarmId + "_timezone", selectedTimezone);
        editor.putString(alarmId + "_recurrence", recurrence);
        editor.apply();

        // Schedule alarm
        scheduleAlarm(alarmId, selectedDateTime.getTimeInMillis(), recurrence);

        Toast.makeText(this,
                isEditMode ? "Alarm updated" : "Alarm created",
                Toast.LENGTH_SHORT
        ).show();

        finish();
    }

    private void scheduleAlarm(String alarmId, long triggerTime, String recurrence) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, AlarmReceiver.class);
        intent.putExtra("ALARM_ID", alarmId);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                alarmId.hashCode(),
                intent,
                PendingIntent.FLAG_IMMUTABLE
        );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
            );
        }
    }
}