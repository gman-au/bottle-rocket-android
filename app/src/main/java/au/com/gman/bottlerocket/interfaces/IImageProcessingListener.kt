package au.com.gman.bottlerocket.interfaces

import au.com.gman.bottlerocket.domain.ImageEnhancementResponse

interface IImageProcessingListener {
    fun onProcessingSuccess(processedResponse: ImageEnhancementResponse)

    fun onProcessingFailure()
}