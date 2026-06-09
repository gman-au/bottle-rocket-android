package au.com.gman.bottlerocket.injection

import au.com.gman.bottlerocket.edgeDetection.ContourPointDetector
import au.com.gman.bottlerocket.interfaces.IEdgeDetector
import au.com.gman.bottlerocket.interfaces.IScribzeeMarkerDetector
import au.com.gman.bottlerocket.scanning.ScribzeeMarkerDetector
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class MarkerDetectionModule {
    @Binds
    abstract fun bindEdgeDetectionModule(
        edgeDetector: ContourPointDetector
    ): IEdgeDetector

    @Binds
    abstract fun bindScribzeeMarkerDetectionModule(
        scribzeeMarkerDetector: ScribzeeMarkerDetector
    ): IScribzeeMarkerDetector
}