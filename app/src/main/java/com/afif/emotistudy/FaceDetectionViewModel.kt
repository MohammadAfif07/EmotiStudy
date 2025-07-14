package com.afif.emotistudy

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.face.Face
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class FaceDetectionViewModel : ViewModel() {

    // LiveData States
    private val _moodState = MutableLiveData<MoodState>()
    val moodState: LiveData<MoodState> = _moodState

    private val _pomodoroState = MutableLiveData<PomodoroState>(PomodoroState.Inactive)
    val pomodoroState: LiveData<PomodoroState> = _pomodoroState

    private val _faceRectangles = MutableLiveData<FaceRectangles>()
    val faceRectangles: LiveData<FaceRectangles> = _faceRectangles

    // Camera State (Fixed)
    private val _isCameraRunning = MutableLiveData(false)
    val isCameraRunning: LiveData<Boolean> = _isCameraRunning

    // Coroutine Jobs
    private var pomodoroJob: Job? = null

    // Sealed Classes
    sealed class MoodState {
        data class Detected(val mood: String, val source: String) : MoodState()
        data class Error(val message: String) : MoodState()
        object Processing : MoodState()
    }

    sealed class PomodoroState {
        object Inactive : PomodoroState()
        data class Running(val minutes: Long, val seconds: Long) : PomodoroState()
        data class Finished(val message: String) : PomodoroState()
    }

    data class FaceRectangles(val faces: List<Face>, val width: Int, val height: Int)

    // Camera Control (Fixed)
    fun setCameraRunning(running: Boolean) {
        _isCameraRunning.value = running
    }

    // Face Detection Logic
    fun processFaces(faces: List<Face>, width: Int, height: Int) {
        _faceRectangles.value = FaceRectangles(faces, width, height)

        if (faces.isEmpty()) {
            _moodState.value = MoodState.Error("No face detected")
            return
        }

        _moodState.value = MoodState.Processing

        val avgSmile = faces.mapNotNull { it.smilingProbability }.average()
        val mood = when {
            avgSmile > 0.6 -> "Happy 😊"
            avgSmile > 0.3 -> "Neutral 😐"
            else -> "Sad 😔"
        }

        _moodState.value = MoodState.Detected(mood, "Face Detection")
    }

    // Voice Analysis Logic
    fun processVoiceInput(text: String) {
        _moodState.value = MoodState.Processing

        val mood = when {
            text.contains("happy", true) -> "Happy 😊"
            text.contains("sad", true) -> "Sad 😔"
            text.contains("angry", true) -> "Angry 😠"
            else -> "Neutral 😐"
        }

        _moodState.value = MoodState.Detected(mood, "Voice Analysis")
    }

    // Pomodoro Timer Logic
    fun startPomodoroTimer() {
        pomodoroJob?.cancel()
        pomodoroJob = viewModelScope.launch {
            val totalTime = 25 * 60 * 1000L
            var remaining = totalTime

            while (remaining > 0) {
                val minutes = remaining / 60000
                val seconds = (remaining % 60000) / 1000
                _pomodoroState.value = PomodoroState.Running(minutes, seconds)
                delay(1000)
                remaining -= 1000
            }

            _pomodoroState.value = PomodoroState.Finished(
                when ((_moodState.value as? MoodState.Detected)?.mood) {
                    "Happy 😊" -> "Great job! Keep up the good work!"
                    "Neutral 😐" -> "Session complete! Stay focused!"
                    else -> "Well done! Take a short break!"
                }
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        pomodoroJob?.cancel()
    }
}