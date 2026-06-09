package au.com.gman.bottlerocket.interfaces
import androidx.camera.core.ImageAnalysis

interface IDetectionArbiter : ImageAnalysis.Analyzer {
    fun setListener(listener: ICaptureDetectionListener)
}