package au.com.gman.bottlerocket

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Path
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
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
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var qrCodeDetector: QrCodeDetector

    private lateinit var previewView: PreviewView

    private lateinit var overlayView: PageCaptureOverlayView
    private lateinit var statusText: TextView
    private lateinit var captureButton: Button
    private lateinit var cameraExecutor: ExecutorService

    private var imageCapture: ImageCapture? = null
    private var matchFound = false
    private var lastQrData: String? = null
    private var lastPageOverlayPath: Path? = null

    // Services
    private val apiService = ApiService("https://your-backend-url.com")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        previewView = findViewById(R.id.previewView)
        overlayView = findViewById(R.id.overlayView)
        statusText = findViewById(R.id.statusText)
        captureButton = findViewById(R.id.captureButton)

        cameraExecutor = Executors.newSingleThreadExecutor()

        captureButton.setOnClickListener {
            takePhoto()
        }

        qrCodeDetector.setListener(object : ITemplateListener {
            override fun onDetectionSuccess(matchedTemplate: TemplateMatchResponse) {
                runOnUiThread {

                    overlayView.post {
                        android.util.Log.d("OVERLAY", "Preview size: ${overlayView.width} x ${overlayView.height}")
                    }

                    matchFound = matchedTemplate.matchFound
                    captureButton.isEnabled = matchFound

                    // set bounding box
                    lastPageOverlayPath = matchedTemplate.overlay

                    statusText.text = when (matchFound) {
                        true -> matchedTemplate.qrCode
                        false -> matchedTemplate.qrCode ?: "No code found"
                    }
                    statusText.setBackgroundColor(
                        when (matchFound) {
                            true -> 0x8000FF00.toInt()
                            false -> 0x80FFA500.toInt()
                        })

                    overlayView.setOverlayPath(lastPageOverlayPath)
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
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
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
                    val previewWidth = overlayView.width
                    val previewHeight = overlayView.height
                    android.util.Log.d(TAG, "Setting preview size: ${previewWidth}x${previewHeight}")
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

    private fun saveImage(bitmap: android.graphics.Bitmap, qrData: String) {
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
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 95, outputStream)
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