package au.com.gman.bottlerocket.injection
import au.com.gman.bottlerocket.interfaces.IEdgeDetector
import au.com.gman.bottlerocket.interfaces.IQrCodeHandler
import au.com.gman.bottlerocket.interfaces.IQrCodeTemplateMatcher
import au.com.gman.bottlerocket.interfaces.IQrPositionalValidator
import au.com.gman.bottlerocket.interfaces.IQrTemplateCache
import au.com.gman.bottlerocket.qrCode.QrCodeTemplateMatcher
import au.com.gman.bottlerocket.qrCode.QrCodeHandler
import au.com.gman.bottlerocket.qrCode.ContourEdgeDetector
import au.com.gman.bottlerocket.qrCode.QrPositionalValidator
import au.com.gman.bottlerocket.qrCode.QrTemplateCache
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
//        templateMapper: QrCodeTemplateMatcher
        templateMapper: QrCodeTemplateMatcher
    ) : IQrCodeTemplateMatcher

    @Binds
    abstract fun bindQrPositionalValidator(
        qrPositionalValidator: QrPositionalValidator
    ) : IQrPositionalValidator

    @Binds
    abstract fun bindQrEdgeDetector(
        contourEdgeDetector: ContourEdgeDetector
    ) : IEdgeDetector

    @Singleton
    @Binds
    abstract fun bindQrTemplateCache(
        qrTemplateCache: QrTemplateCache
    ): IQrTemplateCache
}