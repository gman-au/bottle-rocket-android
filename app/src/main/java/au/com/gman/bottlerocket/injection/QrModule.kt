package au.com.gman.bottlerocket.injection
import au.com.gman.bottlerocket.interfaces.IQrCodeHandler
import au.com.gman.bottlerocket.interfaces.IRocketbookQrCodeTemplateMatcher
import au.com.gman.bottlerocket.interfaces.IQrPositionalValidator
import au.com.gman.bottlerocket.interfaces.IQrTemplateCache
import au.com.gman.bottlerocket.qrCode.QrCodeHandler
import au.com.gman.bottlerocket.qrCode.QrPositionalValidator
import au.com.gman.bottlerocket.qrCode.QrTemplateCache
import au.com.gman.bottlerocket.qrCode.RocketbookQrCodeTemplateMatcher
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class QrModule {

    @Binds
    abstract fun bindQrCodeHandler(
        qrCodeHandler: QrCodeHandler
    ) : IQrCodeHandler

    @Binds
    abstract fun bindQrCodeTemplateMatcher(
        templateMapper: RocketbookQrCodeTemplateMatcher
    ) : IRocketbookQrCodeTemplateMatcher

    @Binds
    abstract fun bindQrPositionalValidator(
        qrPositionalValidator: QrPositionalValidator
    ) : IQrPositionalValidator

    @Singleton
    @Binds
    abstract fun bindQrTemplateCache(
        qrTemplateCache: QrTemplateCache
    ): IQrTemplateCache
}