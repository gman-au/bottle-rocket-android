package au.com.gman.bottlerocket.qrCode

import au.com.gman.bottlerocket.contracts.PageTemplateSummary
import au.com.gman.bottlerocket.interfaces.IQrTemplateCache
import au.com.gman.bottlerocket.interfaces.IRetrofitApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QrTemplateCache @Inject constructor(
    private val retrofitApi: IRetrofitApi
) : IQrTemplateCache {
    private var templates: List<PageTemplateSummary> = emptyList()
    private var isLoaded = false

    companion object {
        private const val TEMPLATE_LOAD_TIMEOUT_MS = 5000L // 5 seconds
    }

    override suspend fun loadTemplates(): Result<List<PageTemplateSummary>> {
        return withContext(Dispatchers.IO) {
            try {
                withTimeout(TEMPLATE_LOAD_TIMEOUT_MS) {
                    val response = retrofitApi.apiFetchPageTemplates()

                    if (response.isSuccessful && response.body() != null) {
                        val body = response.body()!!
                        if (body.isSuccess()) {
                            templates = body.templates.toList()
                            isLoaded = true
                            Result.success(templates)
                        } else {
                            Result.failure(Exception(body.errorMessage))
                        }
                    } else {
                        Result.failure(Exception("HTTP Error: ${response.code()}"))
                    }
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override fun getTemplates(): List<PageTemplateSummary> = templates

    override fun isTemplatesLoaded(): Boolean = isLoaded
}