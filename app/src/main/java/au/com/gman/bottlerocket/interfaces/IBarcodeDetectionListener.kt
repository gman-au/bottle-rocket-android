package au.com.gman.bottlerocket.interfaces

import au.com.gman.bottlerocket.domain.CaptureDetectionResult

interface IBarcodeDetectionListener {
    fun onDetectionSuccess(captureDetectionResult: CaptureDetectionResult)
}