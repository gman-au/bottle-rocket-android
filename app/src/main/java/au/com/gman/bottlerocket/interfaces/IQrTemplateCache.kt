package au.com.gman.bottlerocket.interfaces

import au.com.gman.bottlerocket.contracts.PageTemplateSummary

interface IQrTemplateCache {
    suspend fun loadTemplates(): Result<List<PageTemplateSummary>>

    fun getTemplates(): List<PageTemplateSummary>

    fun isTemplatesLoaded(): Boolean
}