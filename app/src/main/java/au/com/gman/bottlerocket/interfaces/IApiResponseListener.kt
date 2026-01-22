package au.com.gman.bottlerocket.interfaces

import au.com.gman.bottlerocket.contracts.ConnectionTestResponse
import au.com.gman.bottlerocket.contracts.FetchPageTemplatesResponse
import au.com.gman.bottlerocket.contracts.ProcessCaptureResponse

interface IApiResponseListener {
    fun onApiProcessCaptureSuccess(response: ProcessCaptureResponse) = Unit

    fun onApiConnectionTestSuccess(response: ConnectionTestResponse) = Unit

    fun onApiFetchTemplatesSuccess(response: FetchPageTemplatesResponse) = Unit

    fun onApiResponseFailure(response: IApiResponse) = Unit
}