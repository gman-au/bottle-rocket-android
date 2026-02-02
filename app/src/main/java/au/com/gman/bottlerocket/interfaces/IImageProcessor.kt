package au.com.gman.bottlerocket.interfaces

import au.com.gman.bottlerocket.domain.CaptureDetectionResult
import java.io.File

interface IImageProcessor {
    fun setListener(listener: IImageProcessingListener)

    fun processImage(imageFile: File, lastCaptureDetectionResult: CaptureDetectionResult)
}