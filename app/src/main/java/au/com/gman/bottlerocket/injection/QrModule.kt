package au.com.gman.bottlerocket.injection
import au.com.gman.bottlerocket.interfaces.IEdgeDetector
import au.com.gman.bottlerocket.interfaces.IQrCodeHandler
import au.com.gman.bottlerocket.interfaces.IQrCodeTemplateMatcher
import au.com.gman.bottlerocket.qrCode.EdgeDetector
import au.com.gman.bottlerocket.qrCode.QrCodeHandler
import au.com.gman.bottlerocket.qrCode.QrCodeTemplateMatcher
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class QrModule {

    @Binds
    abstract fun bindQrCodeHandler(
        qrCodeHandler: QrCodeHandler
    ) : IQrCodeHandler

    @Binds
    abstract fun bindQrCodeTemplateMatcher(
        templateMapper: QrCodeTemplateMatcher
    ) : IQrCodeTemplateMatcher

    @Binds
    abstract fun bindEdgeDetector(
        edgeDetector: EdgeDetector
    ) : IEdgeDetector
}