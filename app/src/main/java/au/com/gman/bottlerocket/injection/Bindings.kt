package au.com.gman.bottlerocket.injection
import au.com.gman.bottlerocket.imaging.ImageEnhancer
import au.com.gman.bottlerocket.imaging.ImageProcessor
import au.com.gman.bottlerocket.scanning.BarcodeDetector
import au.com.gman.bottlerocket.imaging.PageTemplateRescaler
import au.com.gman.bottlerocket.scanning.RocketBoundingBoxMedianFilter
import au.com.gman.bottlerocket.scanning.ScreenDimensions
import au.com.gman.bottlerocket.scanning.SteadyFrameIndicator
import au.com.gman.bottlerocket.scanning.ViewportRescaler
import au.com.gman.bottlerocket.interfaces.IBarcodeDetector
import au.com.gman.bottlerocket.interfaces.IImageEnhancer
import au.com.gman.bottlerocket.interfaces.IImageProcessor
import au.com.gman.bottlerocket.interfaces.IPageTemplateRescaler
import au.com.gman.bottlerocket.interfaces.IQrCodeHandler
import au.com.gman.bottlerocket.interfaces.IQrCodeTemplateMatcher
import au.com.gman.bottlerocket.interfaces.IRocketBoundingBoxMedianFilter
import au.com.gman.bottlerocket.interfaces.IScreenDimensions
import au.com.gman.bottlerocket.interfaces.ISteadyFrameIndicator
import au.com.gman.bottlerocket.interfaces.IViewportRescaler
import au.com.gman.bottlerocket.qrCode.QrCodeHandler
import au.com.gman.bottlerocket.qrCode.QrCodeTemplateMatcher
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

    @Binds
    abstract fun bindPageTemplateRescaler(
        pageTemplateRescaler: PageTemplateRescaler
    ) : IPageTemplateRescaler

    @Binds
    abstract fun bindImageProcessor(
        imageProcessor: ImageProcessor
    ) : IImageProcessor

    @Binds
    abstract fun bindImageEnhancer(
        imageEnhancer: ImageEnhancer
    ) : IImageEnhancer

    @Singleton
    @Binds
    abstract fun bindSteadyFrameIndicator(
        steadyFrameIndicator: SteadyFrameIndicator
    ) : ISteadyFrameIndicator

    @Singleton
    @Binds
    abstract fun bindScreenDimensions(
        screenDimensions: ScreenDimensions
    ) : IScreenDimensions

    @Singleton
    @Binds
    abstract fun bindRocketBoundingBoxMedianFilter(
        rocketBoundingBoxMedianFilter: RocketBoundingBoxMedianFilter
    ) : IRocketBoundingBoxMedianFilter
}