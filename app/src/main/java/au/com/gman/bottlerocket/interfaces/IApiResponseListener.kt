package au.com.gman.bottlerocket.interfaces

import au.com.gman.bottlerocket.contracts.ProcessCaptureResponse

interface IApiResponseListener {
    fun onApiProcessCaptureSuccess(response: ProcessCaptureResponse)

    fun onApiResponseFailure(response: ProcessCaptureResponse)
}