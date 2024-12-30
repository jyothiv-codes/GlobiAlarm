package edu.sjsu.android.globialarm;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class AlarmDismissActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm_dismiss);

        findViewById(R.id.dismissButton).setOnClickListener(v -> {
            AlarmService.stopAlarm(this);
            finish();
        });
    }
}