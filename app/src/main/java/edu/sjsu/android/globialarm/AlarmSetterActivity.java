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
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.checkbox.MaterialCheckBox;
import android.widget.Filter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Calendar;
import java.util.TimeZone;
import java.util.UUID;

public class AlarmSetterActivity extends AppCompatActivity {
    private EditText alarmName;
    private Button dateButton, timeButton, saveButton;
    private AutoCompleteTextView timezoneAutoComplete;
    private AutoCompleteTextView recurrenceAutoComplete;
    private MaterialCardView weekDaysContainer;
    private MaterialCheckBox[] dayCheckboxes;
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
// Enable the Up button (back arrow) in the action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (!alarmManager.canScheduleExactAlarms()) {
                Intent intent = new Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                intent.setData(android.net.Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            }
        }

        preferences = getSharedPreferences("AlarmPrefs", MODE_PRIVATE);
        selectedDateTime = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

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
        setupWeekDayCheckboxes();
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
        weekDaysContainer = findViewById(R.id.weekDaysContainer);

        dayCheckboxes = new MaterialCheckBox[]{
                findViewById(R.id.sunday),
                findViewById(R.id.monday),
                findViewById(R.id.tuesday),
                findViewById(R.id.wednesday),
                findViewById(R.id.thursday),
                findViewById(R.id.friday),
                findViewById(R.id.saturday)
        };

        if (isEditMode) {
            saveButton.setText("Update Alarm");
        }
    }

    private void setupTimeZoneSpinner() {
        String[] timeZones = TimeZone.getAvailableIDs();
        List<String> timeZoneList = new ArrayList<>(Arrays.asList(timeZones));

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.dropdown_item, timeZoneList) {
            @Override
            public Filter getFilter() {
                return new Filter() {
                    @Override
                    protected FilterResults performFiltering(CharSequence constraint) {
                        FilterResults results = new FilterResults();

                        if (constraint == null || constraint.length() == 0) {
                            ArrayList<String> list = new ArrayList<>(Arrays.asList(timeZones));
                            results.values = list;
                            results.count = list.size();
                        } else {
                            ArrayList<String> filteredList = new ArrayList<>();
                            String filterPattern = constraint.toString().toLowerCase().trim();

                            for (String timezone : timeZones) {
                                if (timezone.toLowerCase().contains(filterPattern)) {
                                    filteredList.add(timezone);
                                }
                            }

                            results.values = filteredList;
                            results.count = filteredList.size();
                        }

                        return results;
                    }

                    @SuppressWarnings("unchecked")
                    @Override
                    protected void publishResults(CharSequence constraint, FilterResults results) {
                        clear();
                        if (results.values != null) {
                            addAll((ArrayList<String>) results.values);
                        }
                        notifyDataSetChanged();
                    }
                };
            }
        };

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
        recurrenceAutoComplete.setText(RECURRENCE_OPTIONS[0], false);

        recurrenceAutoComplete.setOnItemClickListener((parent, view, position, id) -> {
            String selected = parent.getItemAtPosition(position).toString();
            weekDaysContainer.setVisibility(
                    selected.equals("Weekly") ? View.VISIBLE : View.GONE
            );
        });
    }

    private void setupWeekDayCheckboxes() {
        // Initialize with current day checked if it's a new alarm
        if (!isEditMode) {
            int currentDay = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1;
            dayCheckboxes[currentDay].setChecked(true);
        }
    }

    private void setupClickListeners() {
        dateButton.setOnClickListener(v -> showDatePicker());
        timeButton.setOnClickListener(v -> showTimePicker());
        saveButton.setOnClickListener(v -> saveAlarm());
        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> onBackPressed());
    }

    private void loadExistingAlarmData() {
        String name = getIntent().getStringExtra("ALARM_NAME");
        long time = getIntent().getLongExtra("ALARM_TIME", 0);
        String timezone = getIntent().getStringExtra("ALARM_TIMEZONE");
        String recurrence = getIntent().getStringExtra("ALARM_RECURRENCE");

        alarmName.setText(name);

        // Convert UTC time to the alarm's timezone
        TimeZone targetTimezone = TimeZone.getTimeZone(timezone);
        Calendar targetTime = Calendar.getInstance(targetTimezone);
        targetTime.setTimeInMillis(time - targetTimezone.getOffset(time));

        dateButton.setText(String.format("%d/%d/%d",
                targetTime.get(Calendar.DAY_OF_MONTH),
                targetTime.get(Calendar.MONTH) + 1,
                targetTime.get(Calendar.YEAR)));

        timeButton.setText(String.format("%02d:%02d",
                targetTime.get(Calendar.HOUR_OF_DAY),
                targetTime.get(Calendar.MINUTE)));

        selectedDateTime = targetTime;
        timezoneAutoComplete.setText(timezone);

        if (recurrence != null) {
            recurrenceAutoComplete.setText(recurrence, false);
            if (recurrence.equals("Weekly")) {
                weekDaysContainer.setVisibility(View.VISIBLE);
                String weekdays = preferences.getString(editAlarmId + "_weekdays", "0000000");
                for (int i = 0; i < 7; i++) {
                    dayCheckboxes[i].setChecked(weekdays.charAt(i) == '1');
                }
            }
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

        if (recurrence.equals("Weekly")) {
            boolean anyDaySelected = false;
            for (MaterialCheckBox checkbox : dayCheckboxes) {
                if (checkbox.isChecked()) {
                    anyDaySelected = true;
                    break;
                }
            }
            if (!anyDaySelected) {
                Toast.makeText(this, "Please select at least one day", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            Toast.makeText(this, "Please allow exact alarms permission", Toast.LENGTH_LONG).show();
            return;
        }

        // Convert time to the selected timezone
        TimeZone targetTimezone = TimeZone.getTimeZone(selectedTimezone);
        Calendar targetTime = Calendar.getInstance(targetTimezone);

        // Transfer the time components
        targetTime.set(Calendar.YEAR, selectedDateTime.get(Calendar.YEAR));
        targetTime.set(Calendar.MONTH, selectedDateTime.get(Calendar.MONTH));
        targetTime.set(Calendar.DAY_OF_MONTH, selectedDateTime.get(Calendar.DAY_OF_MONTH));
        targetTime.set(Calendar.HOUR_OF_DAY, selectedDateTime.get(Calendar.HOUR_OF_DAY));
        targetTime.set(Calendar.MINUTE, selectedDateTime.get(Calendar.MINUTE));
        targetTime.set(Calendar.SECOND, 0);
        targetTime.set(Calendar.MILLISECOND, 0);

        // Convert to UTC for storage
        long utcTime = targetTime.getTimeInMillis() + targetTimezone.getOffset(targetTime.getTimeInMillis());

        SharedPreferences.Editor editor = preferences.edit();
        String alarmId;

        if (isEditMode) {
            alarmId = editAlarmId;
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
        editor.putLong(alarmId + "_time", utcTime);
        editor.putString(alarmId + "_timezone", selectedTimezone);
        editor.putString(alarmId + "_recurrence", recurrence);

        if (recurrence.equals("Weekly")) {
            StringBuilder daysString = new StringBuilder();
            for (MaterialCheckBox checkbox : dayCheckboxes) {
                daysString.append(checkbox.isChecked() ? "1" : "0");
            }
            editor.putString(alarmId + "_weekdays", daysString.toString());
        }

        editor.apply();

        // Schedule alarm using UTC time
        scheduleAlarm(alarmId, utcTime, recurrence);

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

        if (recurrence.equals("Weekly")) {
            String weekdays = preferences.getString(alarmId + "_weekdays", "0000000");
            Calendar nextAlarm = Calendar.getInstance();
            nextAlarm.setTimeInMillis(triggerTime);

            // Find next occurrence
            for (int i = 0; i < 7; i++) {
                int dayOfWeek = (nextAlarm.get(Calendar.DAY_OF_WEEK) - 1 + i) % 7;
                if (weekdays.charAt(dayOfWeek) == '1') {
                    if (i > 0) {
                        nextAlarm.add(Calendar.DAY_OF_YEAR, i);
                    }
                    triggerTime = nextAlarm.getTimeInMillis();
                    break;
                }
            }
        }

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
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}