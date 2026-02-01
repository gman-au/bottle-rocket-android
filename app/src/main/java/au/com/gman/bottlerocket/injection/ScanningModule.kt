package au.com.gman.bottlerocket.injection

import au.com.gman.bottlerocket.scanning.BarcodeDetector
import au.com.gman.bottlerocket.scanning.RocketBoundingBoxMedianFilter
import au.com.gman.bottlerocket.scanning.ScreenDimensions
import au.com.gman.bottlerocket.scanning.SteadyFrameIndicator
import au.com.gman.bottlerocket.scanning.ViewportRescaler
import au.com.gman.bottlerocket.interfaces.ICaptureArtifactDetector
import au.com.gman.bottlerocket.interfaces.IRocketBoundingBoxMedianFilter
import au.com.gman.bottlerocket.interfaces.IScreenDimensions
import au.com.gman.bottlerocket.interfaces.ISteadyFrameIndicator
import au.com.gman.bottlerocket.interfaces.IViewportRescaler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ScanningModule {

    @Binds
    abstract fun bindBarCodeDetector(
        barcodeDetector: BarcodeDetector
    ): ICaptureArtifactDetector

    @Singleton
    @Binds
    abstract fun bindRocketBoundingBoxMedianFilter(
        rocketBoundingBoxMedianFilter: RocketBoundingBoxMedianFilter
    ): IRocketBoundingBoxMedianFilter

    @Singleton
    @Binds
    abstract fun bindScreenDimensions(
        screenDimensions: ScreenDimensions
    ): IScreenDimensions

    @Singleton
    @Binds
    abstract fun bindSteadyFrameIndicator(
        steadyFrameIndicator: SteadyFrameIndicator
    ): ISteadyFrameIndicator

    @Binds
    abstract fun bindViewportRescaler(
        viewportRescaler: ViewportRescaler
    ): IViewportRescaler
}