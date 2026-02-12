package au.com.gman.bottlerocket.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.PointF
import android.hardware.camera2.CaptureRequest
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.Range
import android.widget.ImageButton
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import au.com.gman.bottlerocket.PageCaptureOverlayView
import au.com.gman.bottlerocket.R
import au.com.gman.bottlerocket.domain.CaptureDetectionResult
import au.com.gman.bottlerocket.domain.ImageEnhancementResponse
import au.com.gman.bottlerocket.domain.RocketBoundingBox
import au.com.gman.bottlerocket.extensions.toApiString
import au.com.gman.bottlerocket.interfaces.ICaptureDetectionListener
import au.com.gman.bottlerocket.interfaces.ICaptureArtifactDetector
import au.com.gman.bottlerocket.interfaces.IFileSaveListener
import au.com.gman.bottlerocket.interfaces.IFileIo
import au.com.gman.bottlerocket.interfaces.IImageProcessingListener
import au.com.gman.bottlerocket.interfaces.IImageProcessor
import au.com.gman.bottlerocket.interfaces.IScreenDimensions
import au.com.gman.bottlerocket.interfaces.ISteadyFrameIndicator
import au.com.gman.bottlerocket.interfaces.ISteadyFrameListener
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Inject

@AndroidEntryPoint
class CaptureActivity : AppCompatActivity() {

    @Inject
    lateinit var captureArtifactDetector: ICaptureArtifactDetector

    @Inject
    lateinit var imageProcessor: IImageProcessor

    @Inject
    lateinit var screenDimensions: IScreenDimensions

    @Inject
    lateinit var steadyFrameIndicator: ISteadyFrameIndicator

    @Inject
    lateinit var fileIo: IFileIo

    private lateinit var previewView: PreviewView

    private lateinit var overlayView: PageCaptureOverlayView

    private lateinit var cancelButton: ImageButton

    private lateinit var cameraExecutor: ExecutorService

    private lateinit var lastCaptureDetectionResult: CaptureDetectionResult

    private var lastEnhancedQrBoundingBox: RocketBoundingBox? = null

    private var imageCapture: ImageCapture? = null

    private var matchFound = false

    private var outOfBounds = false

    private var codeFound = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_capture_image)
        checkStoragePermission()

        previewView = findViewById(R.id.previewView)
        overlayView = findViewById(R.id.overlayView)
        cancelButton = findViewById(R.id.cancelButton)

        cameraExecutor = Executors.newSingleThreadExecutor()

        cancelButton.setOnClickListener {
            finish()
        }

        steadyFrameIndicator.setProcessing(false)

        captureArtifactDetector
            .setListener(object : ICaptureDetectionListener {
                override fun onDetectionSuccess(captureDetectionResult: CaptureDetectionResult) {
                    runOnUiThread {

                        codeFound = captureDetectionResult.codeFound
                        matchFound = captureDetectionResult.matchFound
                        outOfBounds = captureDetectionResult.outOfBounds

                        if (codeFound) {
                            if (outOfBounds)
                                steadyFrameIndicator.setOutOfBounds(true)
                            else
                                steadyFrameIndicator.setOutOfBounds(false)
                        } else
                            steadyFrameIndicator.setOutOfBounds(false)

                        if (matchFound) {
                            steadyFrameIndicator.increment()
                            overlayView.setUnmatchedQrCode(null)
                        } else {
                            if (codeFound) {
                                overlayView.setUnmatchedQrCode(captureDetectionResult.qrCode)
                            } else {
                                overlayView.setUnmatchedQrCode(null)
                            }
                            steadyFrameIndicator.reset()
                        }

                        lastCaptureDetectionResult = captureDetectionResult

                        overlayView.setPageOverlayBox(captureDetectionResult.pageOverlayPathPreview)
                        overlayView.setIndicatorBoxes(captureDetectionResult.indicatorBoxesPreview)
                    }
                }
            })

        imageProcessor
            .setListener(object : IImageProcessingListener {
                override fun onProcessingSuccess(processedResponse: ImageEnhancementResponse) {
                    lastEnhancedQrBoundingBox = processedResponse.scaledQrBox
                    fileIo
                        .saveImage(
                            processedResponse.bitmap,
                            FILENAME_FORMAT,
                            contentResolver
                        )
                }

                override fun onProcessingFailure() {
                    runOnUiThread {
                        Toast.makeText(
                            baseContext,
                            "There was an error processing the image",
                            Toast.LENGTH_SHORT
                        )
                            .show()
                    }
                }
            })

        steadyFrameIndicator
            .setListener(object : ISteadyFrameListener {
                override fun onSteadyResult() {
                    // prevent further activity
                    steadyFrameIndicator.setProcessing(true)

                    // take the photo!
                    takePhoto()

                    // reset
                    steadyFrameIndicator.reset()
                }
            })

        fileIo
            .setSaveListener(object : IFileSaveListener {
                override fun onFileSaveSuccess(uri: Uri) {
                    steadyFrameIndicator.setProcessing(false)
                    val intent = Intent(this@CaptureActivity, PreviewActivity::class.java)
                    intent.putExtra("imagePath", uri);
                    intent.putExtra("qrCode", lastCaptureDetectionResult.qrCode)
                    intent.putExtra("qrBoundingBox", lastEnhancedQrBoundingBox?.toApiString())
                    startActivity(intent);
                }

                override fun onFileSaveFailure() {
                    runOnUiThread {
                        Toast.makeText(
                            baseContext,
                            "There was an error saving the image",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    steadyFrameIndicator.setProcessing(false)
                }
            })

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }
    }

    @OptIn(ExperimentalCamera2Interop::class)
    private fun startCamera() {
        val cameraProviderFuture =
            ProcessCameraProvider
                .getInstance(this)

        cameraProviderFuture
            .addListener({
                val cameraProvider =
                    cameraProviderFuture
                        .get()

                val cameraSelector =
                    CameraSelector
                        .DEFAULT_BACK_CAMERA

                // Create a consistent resolution selector for all use cases
                val resolutionSelector =
                    androidx.camera.core.resolutionselector.ResolutionSelector.Builder()
                        .setAspectRatioStrategy(
                            androidx.camera.core.resolutionselector.AspectRatioStrategy(
                                androidx.camera.core.AspectRatio.RATIO_4_3,
                                androidx.camera.core.resolutionselector.AspectRatioStrategy.FALLBACK_RULE_AUTO
                            )
                        )
                        .build()

                val preview =
                    Preview
                        .Builder()
                        .setResolutionSelector(resolutionSelector)
                        .build()
                        .also {
                            it
                                .setSurfaceProvider(previewView.surfaceProvider)
                        }

                imageCapture =
                    ImageCapture
                        .Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                        .setTargetRotation(windowManager.defaultDisplay.rotation)
                        .setResolutionSelector(resolutionSelector)
                        .build()

                val imageAnalyzer =
                    ImageAnalysis
                        .Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setResolutionSelector(resolutionSelector)
                        .build()
                        .also {
                            it
                                .setAnalyzer(cameraExecutor, captureArtifactDetector)
                        }

                try {
                    cameraProvider
                        .unbindAll()

                    val camera =
                        cameraProvider
                            .bindToLifecycle(
                                this,
                                cameraSelector,
                                preview,
                                imageCapture,
                                imageAnalyzer
                            )

                    // enable this for flash
//                    camera.cameraControl.enableTorch(true)

                    val camera2Control =
                        Camera2CameraControl
                            .from(camera.cameraControl)

                    camera2Control
                        .setCaptureRequestOptions(
                            CaptureRequestOptions.Builder()
                                .setCaptureRequestOption(
                                    CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
                                    Range(30, 30)
                                )
                                .build()
                        )

                    overlayView
                        .post {
                            screenDimensions
                                .setTargetSize(
                                    PointF(
                                        previewView.measuredWidth.toFloat(),
                                        previewView.measuredHeight.toFloat(),
                                    )
                                )
                        }

                } catch (exc: Exception) {
                    Toast
                        .makeText(this, "Camera binding failed", Toast.LENGTH_SHORT).show()
                }

            }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        val photoFile = File(
            cacheDir,
            SimpleDateFormat(FILENAME_FORMAT, Locale.US)
                .format(System.currentTimeMillis()) + ".jpg"
        )

        val outputOptions =
            ImageCapture
                .OutputFileOptions
                .Builder(photoFile)
                .build()

        imageCapture
            .takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(this),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onError(exc: ImageCaptureException) {
                        Toast.makeText(
                            baseContext, "Capture failed: ${exc.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        cameraExecutor
                            .execute {
                                imageProcessor
                                    .processImage(
                                        photoFile,
                                        lastCaptureDetectionResult
                                    )
                            }
                    }
                }
            )
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted - proceed with save
            } else {
                Toast.makeText(this, "Storage permission required", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkStoragePermission() {
        Log.d(TAG, "Android SDK: ${Build.VERSION.SDK_INT}")
        Log.d(TAG, "Android Version: ${Build.VERSION.RELEASE}")

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            val hasPermission = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED

            Log.d(TAG, "Has WRITE_EXTERNAL_STORAGE permission: $hasPermission")

            if (!hasPermission) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    STORAGE_PERMISSION_CODE
                )
            }
        } else {
            Log.d(TAG, "Android 10+, no permission needed for MediaStore")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "CaptureActivity"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)

        private const val STORAGE_PERMISSION_CODE = 100
    }
}