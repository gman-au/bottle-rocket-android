package au.com.gman.bottlerocket.activity

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Path
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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
import au.com.gman.bottlerocket.domain.TemplateMatchResponse
import au.com.gman.bottlerocket.imaging.QrCodeDetector
import au.com.gman.bottlerocket.interfaces.ITemplateListener
import au.com.gman.bottlerocket.network.ApiService
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
    lateinit var qrCodeDetector: QrCodeDetector

    private lateinit var previewView: PreviewView

    private lateinit var overlayView: PageCaptureOverlayView
    private lateinit var statusText: TextView

    private lateinit var debugText: TextView
    private lateinit var captureButton: Button

    private lateinit var cancelButton: Button
    private lateinit var cameraExecutor: ExecutorService

    private lateinit var lastMatchedTemplate: TemplateMatchResponse

    private var imageCapture: ImageCapture? = null
    private var matchFound = false
    private var lastQrData: String? = null
    private var lastPageOverlayPath: Path? = null

    private var previewWidth: Int = 0
    private var previewHeight: Int = 0

    // Services
    private val apiService = ApiService("https://your-backend-url.com")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_capture)

        previewView = findViewById(R.id.previewView)
        overlayView = findViewById(R.id.overlayView)
        statusText = findViewById(R.id.statusText)
        debugText = findViewById(R.id.debugText)
        captureButton = findViewById(R.id.captureButton)
        cancelButton = findViewById(R.id.cancelButton)

        cameraExecutor = Executors.newSingleThreadExecutor()

        captureButton.setOnClickListener {
            takePhoto()
        }

        cancelButton.setOnClickListener {
            finish()
        }

        qrCodeDetector.setListener(object : ITemplateListener {
            override fun onDetectionSuccess(matchedTemplate: TemplateMatchResponse) {
                runOnUiThread {

                    matchFound = matchedTemplate.matchFound
                    captureButton.isEnabled = matchFound

                    statusText.text = when (matchFound) {
                        true -> matchedTemplate.qrCode
                        false -> matchedTemplate.qrCode ?: "No code found"
                    }
                    statusText.setBackgroundColor(
                        when (matchFound) {
                            true -> 0x8000FF00.toInt()
                            false -> 0x80FFA500.toInt()
                        })

                    lastMatchedTemplate = matchedTemplate
                    overlayView.setPageOverlayBox(matchedTemplate.pageOverlayPath)
                    overlayView.setQrOverlayPath(matchedTemplate.qrCodeOverlayPath)

                    updateDebugText()
                }
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

    private fun updateDebugText() {
        val builder = StringBuilder()
        builder.appendLine()
        builder.appendLine(lastMatchedTemplate.qrCode)
        builder.appendLine("QR box:")
        builder.appendLine(lastMatchedTemplate.qrCodeOverlayPath.toString())
        builder.appendLine("Page overlay box:")
        builder.appendLine(lastMatchedTemplate.pageOverlayPath.toString())
        builder.appendLine("Preview size: ${previewWidth} x ${previewHeight}")
        debugText.text = builder.toString()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setTargetRotation(windowManager.defaultDisplay.rotation)
                .build()

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, qrCodeDetector)
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture, imageAnalyzer
                )

                overlayView.post {
                    previewWidth = overlayView.width
                    previewHeight = overlayView.height
                    Log.d(TAG, "Setting preview size: ${previewWidth}x${previewHeight}")
                    qrCodeDetector.setPreviewSize(previewWidth, previewHeight)
                }

            } catch (exc: Exception) {
                Toast.makeText(this, "Camera binding failed", Toast.LENGTH_SHORT).show()
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

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Toast.makeText(baseContext, "Capture failed: ${exc.message}", Toast.LENGTH_SHORT).show()
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    Toast.makeText(baseContext, "Processing...", Toast.LENGTH_SHORT).show()

                    /*cameraExecutor.execute {
                        processAndSaveImage(photoFile, lastQrData ?: "", lastQrBoundingBox)
                    }*/
                }
            }
        )
    }

    /*
    private fun processAndSaveImage(imageFile: File, qrData: String, qrBoundingBox: Rect?) {
        try {
            val originalBitmap = BitmapFactory.decodeFile(imageFile.absolutePath)

            Log.d(TAG, "Processing image: ${originalBitmap.width}x${originalBitmap.height}")
            Log.d(TAG, "QR Data: $qrData")
            Log.d(TAG, "QR Box: $qrBoundingBox")

            // Process with QR bounding box for smart cropping
            val processedBitmap = imageProcessor.processImageWithQR(
                originalBitmap,
                qrData,
                qrBoundingBox
            )

            Log.d(TAG, "Processed: ${processedBitmap.width}x${processedBitmap.height}")

            saveImage(processedBitmap, qrData)

            // Upload to backend (optional)
            /*
            apiService.uploadImage(imageFile, qrData, object : ApiService.UploadCallback {
                override fun onSuccess(response: UploadResponse) {
                    runOnUiThread {
                        Toast.makeText(baseContext, "Uploaded!", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onError(message: String) {
                    runOnUiThread {
                        Toast.makeText(baseContext, message, Toast.LENGTH_SHORT).show()
                    }
                }
            })
            */

        } catch (e: Exception) {
            Log.e(TAG, "Processing failed", e)
            runOnUiThread {
                Toast.makeText(baseContext, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } finally {
            imageFile.delete()
        }
    }
    */

    private fun saveImage(bitmap: Bitmap, qrData: String) {
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis())

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/BottleRocket")
            }
            put(MediaStore.Images.Media.DESCRIPTION, "QR: $qrData")
        }

        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        uri?.let {
            contentResolver.openOutputStream(it)?.use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
            }
            runOnUiThread {
                Toast.makeText(baseContext, "Saved: $qrData", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this, "Permissions not granted", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "BottleRocket"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}