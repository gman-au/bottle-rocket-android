package au.com.gman.bottlerocket.injection

import au.com.gman.bottlerocket.edgeDetection.ContourPointDetector
import au.com.gman.bottlerocket.interfaces.IEdgeDetector
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class EdgeDetectionModule {
    @Binds
    abstract fun bindEdgeDetectionModule(
        edgeDetector: ContourPointDetector
    ): IEdgeDetector
}