package com.afif.emotistudy

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.*
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.afif.emotistudy.ml.EmotionClassifier
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.io.ByteArrayOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class FaceDetectionActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var tvMoodResult: TextView
    private lateinit var btnCapture: Button
    private lateinit var btnVoiceMood: Button
    private lateinit var btnPomodoro: Button
    private lateinit var faceOverlayView: FaceOverlayView
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var viewModel: FaceDetectionViewModel
    private lateinit var emotionClassifier: EmotionClassifier

    private val tag = "FaceDetectionActivity"

    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) showPermissionDeniedToast("Camera permission denied")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_face_detection)

        setupViewModel()
        initializeViews()
        emotionClassifier = EmotionClassifier(this)
        setupClickListeners()
        observeViewModel()
        checkCameraPermission()
    }

    private fun setupViewModel() {
        viewModel = ViewModelProvider(this)[FaceDetectionViewModel::class.java]
    }

    private fun initializeViews() {
        previewView = findViewById(R.id.previewView)
        tvMoodResult = findViewById(R.id.tvMoodResult)
        btnCapture = findViewById(R.id.btnCapture)
        btnVoiceMood = findViewById(R.id.btnVoiceMood)
        btnPomodoro = findViewById(R.id.btnPomodoro)
        faceOverlayView = findViewById(R.id.faceOverlayView)
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun setupClickListeners() {
        btnCapture.setOnClickListener { handleCaptureClick() }
        btnVoiceMood.setOnClickListener { showToast("Voice detection unavailable here") }
        btnPomodoro.setOnClickListener { viewModel.startPomodoroTimer() }
    }

    private fun observeViewModel() {
        viewModel.isCameraRunning.observe(this) { isRunning ->
            btnCapture.text = if (isRunning) "Stop" else "Start Face Mood"
        }

        viewModel.faceRectangles.observe(this) { rects ->
            faceOverlayView.setFaces(rects.faces, rects.width, rects.height)
        }

        viewModel.pomodoroState.observe(this) { state ->
            when (state) {
                is FaceDetectionViewModel.PomodoroState.Running ->
                    tvMoodResult.text = "Time Left: %02d:%02d".format(state.minutes, state.seconds)
                is FaceDetectionViewModel.PomodoroState.Finished ->
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                FaceDetectionViewModel.PomodoroState.Inactive ->
                    tvMoodResult.text = "Idle"
            }
        }
    }

    private fun checkCameraPermission() {
        if (!hasCameraPermission()) {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun handleCaptureClick() {
        if (hasCameraPermission()) {
            if (viewModel.isCameraRunning.value == true) {
                showToast("Mood detection already running.")
            } else {
                startCameraCountdown()
            }
        } else {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCameraCountdown() {
        btnCapture.isEnabled = false
        object : CountDownTimer(3000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                tvMoodResult.text = "Starting in ${millisUntilFinished / 1000}..."
            }

            override fun onFinish() {
                tvMoodResult.text = "Detecting..."
                startCamera()
                btnCapture.isEnabled = true
            }
        }.start()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().apply {
                setSurfaceProvider(previewView.surfaceProvider)
            }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .apply {
                    setAnalyzer(cameraExecutor, createFaceAnalyzer())
                }

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                this,
                CameraSelector.DEFAULT_FRONT_CAMERA,
                preview,
                imageAnalyzer
            )

            viewModel.setCameraRunning(true)
        }, ContextCompat.getMainExecutor(this))
    }

    @OptIn(ExperimentalGetImage::class)
    private fun createFaceAnalyzer(): ImageAnalysis.Analyzer {
        val detector = FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .build()
        )

        return ImageAnalysis.Analyzer { imageProxy ->
            val mediaImage = imageProxy.image ?: run {
                imageProxy.close()
                return@Analyzer
            }

            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            detector.process(image)
                .addOnSuccessListener { faces ->
                    viewModel.processFaces(faces, image.width, image.height)

                    if (faces.isNotEmpty()) {
                        val face = faces[0]
                        val bounds = face.boundingBox
                        val bitmap = imageProxyToBitmap(imageProxy) ?: run {
                            imageProxy.close()
                            return@addOnSuccessListener
                        }

                        // 🛡️ Safe cropping
                        val left = bounds.left.coerceAtLeast(0)
                        val top = bounds.top.coerceAtLeast(0)
                        val right = bounds.right.coerceAtMost(bitmap.width)
                        val bottom = bounds.bottom.coerceAtMost(bitmap.height)

                        val width = (right - left).coerceAtLeast(1)
                        val height = (bottom - top).coerceAtLeast(1)

                        val cropped = Bitmap.createBitmap(bitmap, left, top, width, height)

                        val (mood, confidence) = emotionClassifier.classify(cropped)

                        runOnUiThread {
                            tvMoodResult.text = "Detected Mood: $mood\nScore: ${"%.2f".format(confidence)}"
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(tag, "Face detection error", e)
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        }
    }

    private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap? {
        val yBuffer = imageProxy.planes[0].buffer
        val uBuffer = imageProxy.planes[1].buffer
        val vBuffer = imageProxy.planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)
        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, imageProxy.width, imageProxy.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, imageProxy.width, imageProxy.height), 100, out)
        val jpegBytes = out.toByteArray()
        return BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size)
    }

    private fun hasCameraPermission() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun showPermissionDeniedToast(message: String) {
        Toast.makeText(this, "⚠️ $message", Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        viewModel.setCameraRunning(false)
    }
}
