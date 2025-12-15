package au.com.gman.bottlerocket.injection
import au.com.gman.bottlerocket.imaging.BarcodeDetector
import au.com.gman.bottlerocket.imaging.ViewportRescaler
import au.com.gman.bottlerocket.imaging.ScreenDimensions
import au.com.gman.bottlerocket.qrCode.QrCodeTemplateMatcher
import au.com.gman.bottlerocket.interfaces.IBarcodeDetector
import au.com.gman.bottlerocket.interfaces.IViewportRescaler
import au.com.gman.bottlerocket.interfaces.IQrCodeHandler
import au.com.gman.bottlerocket.interfaces.IQrCodeTemplateMatcher
import au.com.gman.bottlerocket.interfaces.IScreenDimensions
import au.com.gman.bottlerocket.qrCode.QrCodeHandler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ImagingModule {

    @Binds
    abstract fun bindTemplateMapper(
        templateMapper: QrCodeTemplateMatcher
    ) : IQrCodeTemplateMatcher

    @Binds
    abstract fun bindBarCodeDetector(
        barcodeDetector: BarcodeDetector
    ) : IBarcodeDetector

    @Binds
    abstract fun bindQrCodeHandler(
        qrCodeHandler: QrCodeHandler
    ) : IQrCodeHandler

    @Binds
    abstract fun bindViewportRescaler(
        viewportRescaler: ViewportRescaler
    ) : IViewportRescaler

    @Singleton
    @Binds
    abstract fun bindScreenDimensions(
        screenDimensions: ScreenDimensions
    ) : IScreenDimensions
}