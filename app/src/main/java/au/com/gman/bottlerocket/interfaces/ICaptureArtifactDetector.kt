package au.com.gman.bottlerocket.interfaces

import androidx.camera.core.ImageProxy
import au.com.gman.bottlerocket.domain.CaptureDetectionResult
import com.google.mlkit.vision.common.InputImage

interface ICaptureArtifactDetector {
    fun capture(
        imageProxy: ImageProxy,
        image: InputImage,
        rotationDegrees: Int,
        imageWidth: Int,
        imageHeight: Int
    ): CaptureDetectionResult
}