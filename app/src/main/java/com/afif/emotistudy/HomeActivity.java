package com.afif.emotistudy;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Random;

public class HomeActivity extends AppCompatActivity {

    private Button btnStartTimer;
    private Button btnDetectMood;
    private Button btnVoiceMood;
    private Button btnTypingMood;

    private final String[] motivationalQuotes = {
            "Keep going, you're doing great!",
            "Every step counts, no matter how small.",
            "Focus. Breathe. Believe.",
            "You’re capable of amazing things.",
            "One hour today can change your future."
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);

        btnDetectMood = findViewById(R.id.btnDetectMood);
        btnStartTimer = findViewById(R.id.btnStartTimer);
        btnVoiceMood = findViewById(R.id.btnVoiceMood);
        btnTypingMood = findViewById(R.id.btnTypingMood);

        // Show random quote on launch
        String launchQuote = motivationalQuotes[new Random().nextInt(motivationalQuotes.length)];
        Toast.makeText(this, launchQuote, Toast.LENGTH_LONG).show();

        // Show mood from previous activity if present
        if (getIntent().hasExtra("detected_mood")) {
            String mood = getIntent().getStringExtra("detected_mood");
            Toast.makeText(this, "🧠 Last mood: " + mood, Toast.LENGTH_SHORT).show();
        }

        btnDetectMood.setOnClickListener(v -> {
            startActivity(new Intent(HomeActivity.this, FaceDetectionActivity.class));
        });

        btnVoiceMood.setOnClickListener(v -> {
            startActivity(new Intent(HomeActivity.this, VoiceMoodActivity.class));
        });

        btnTypingMood.setOnClickListener(v -> {
            startActivity(new Intent(HomeActivity.this, TypingMoodActivity.class));
        });

        btnStartTimer.setOnClickListener(v -> startStudyTimer());
    }

    private void startStudyTimer() {
        new CountDownTimer(5 * 60 * 1000, 1000) {
            public void onTick(long millisUntilFinished) {
                long minutes = millisUntilFinished / 60000;
                long seconds = (millisUntilFinished % 60000) / 1000;
                String timeRemaining = String.format("Time Left: %02d:%02d", minutes, seconds);
                btnStartTimer.setText(timeRemaining);
            }

            public void onFinish() {
                btnStartTimer.setText("Start Study Timer");
                String quote = motivationalQuotes[new Random().nextInt(motivationalQuotes.length)];
                Toast.makeText(HomeActivity.this, "Session complete!\n" + quote, Toast.LENGTH_LONG).show();
            }
        }.start();
    }
}
