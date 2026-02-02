package au.com.gman.bottlerocket.injection
import android.content.Context
import au.com.gman.bottlerocket.edgeDetection.ContourPointDetector
import au.com.gman.bottlerocket.edgeDetection.ScribzeeMarkerDetector
import au.com.gman.bottlerocket.interfaces.IEdgeDetector
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent


@Module
@InstallIn(SingletonComponent::class)
object EdgeDetectionModule {

    @Provides
    @TheContourPointDetector
    fun provideTheContourPointDetector(): IEdgeDetector {
        return ContourPointDetector()
    }

    @Provides
    @TheArtifactPointDetector
    fun provideTheArtifactPointDetector(
        @ApplicationContext context: Context
    ): IEdgeDetector {
        return ScribzeeMarkerDetector()
    }

}