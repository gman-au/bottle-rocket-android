package au.com.gman.bottlerocket.interfaces

import au.com.gman.bottlerocket.domain.CaptureDetectionResult

interface ICaptureDetectionListener {
    fun onDetectionSuccess(captureDetectionResult: CaptureDetectionResult)
}