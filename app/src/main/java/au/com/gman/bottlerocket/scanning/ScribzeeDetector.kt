package au.com.gman.bottlerocket.scanning

import androidx.camera.core.ImageProxy
import au.com.gman.bottlerocket.domain.CaptureDetectionResult
import au.com.gman.bottlerocket.interfaces.ICaptureArtifactDetector
import com.google.mlkit.vision.common.InputImage
import javax.inject.Inject

class ScribzeeDetector @Inject constructor() : ICaptureArtifactDetector {

    override fun capture(
        imageProxy: ImageProxy,
        image: InputImage,
        rotationDegrees: Int,
        imageWidth: Int,
        imageHeight: Int
    ): CaptureDetectionResult {
        return CaptureDetectionResult.EMPTY
    }
}