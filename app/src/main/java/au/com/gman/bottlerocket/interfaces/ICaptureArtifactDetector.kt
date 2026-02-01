package au.com.gman.bottlerocket.interfaces

import androidx.camera.core.ImageAnalysis

interface ICaptureArtifactDetector : ImageAnalysis.Analyzer {
    fun setListener(listener: ICaptureDetectionListener)
}