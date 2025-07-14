package com.afif.emotistudy

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.*

class TypingMoodActivity : AppCompatActivity() {

    private lateinit var etTypingInput: EditText
    private lateinit var tvTypingMood: TextView

    private val keyTimestamps = mutableListOf<Long>()
    private var backspaceCount = 0
    private var startTime: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_typing_mood)

        etTypingInput = findViewById(R.id.etTypingInput)
        tvTypingMood = findViewById(R.id.tvTypingMood)

        startTime = System.currentTimeMillis()

        etTypingInput.addTextChangedListener(object : TextWatcher {
            private var lastTextLength = 0

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                lastTextLength = s?.length ?: 0
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val now = System.currentTimeMillis()
                keyTimestamps.add(now)

                if ((s?.length ?: 0) < lastTextLength) {
                    backspaceCount++
                }

                // Analyze every 10+ characters typed
                if ((s?.length ?: 0) > 10 && keyTimestamps.size % 10 == 0) {
                    analyzeTypingMood()
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun analyzeTypingMood() {
        if (keyTimestamps.size < 2) return

        val durationMinutes = (System.currentTimeMillis() - startTime) / 60000.0
        val wordCount = etTypingInput.text.toString().trim().split("\\s+".toRegex()).size
        val wpm = if (durationMinutes > 0) (wordCount / durationMinutes).toInt() else 0

        val intervals = keyTimestamps.zipWithNext { a, b -> b - a }
        val avgInterval = intervals.average()

        val mood = when {
            wpm > 50 && avgInterval < 200 -> "Focused 🚀"
            wpm < 20 && avgInterval > 800 -> "Tired 😴"
            backspaceCount > 10 -> "Anxious 😟"
            else -> "Neutral 😐"
        }

        tvTypingMood.text = getString(R.string.typing_mood_format, mood)


    }
}
