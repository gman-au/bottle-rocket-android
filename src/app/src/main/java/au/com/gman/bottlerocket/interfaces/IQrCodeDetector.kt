package au.com.gman.bottlerocket.interfaces

import androidx.camera.core.ImageAnalysis

interface IQrCodeDetector : ImageAnalysis.Analyzer {
    fun setListener(listener: ITemplateListener)
}

