package au.com.gman.bottlerocket.injection
import au.com.gman.bottlerocket.imaging.ImageEnhancer
import au.com.gman.bottlerocket.imaging.ImageProcessor
import au.com.gman.bottlerocket.interfaces.IImageEnhancer
import au.com.gman.bottlerocket.interfaces.IImageProcessor
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class ImagingModule {

    @Binds
    abstract fun bindImageEnhancer(
        imageEnhancer: ImageEnhancer
    ) : IImageEnhancer

    @Binds
    abstract fun bindImageProcessor(
        imageProcessor: ImageProcessor
    ) : IImageProcessor
}