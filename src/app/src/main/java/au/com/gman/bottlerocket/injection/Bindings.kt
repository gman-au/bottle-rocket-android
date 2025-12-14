package au.com.gman.bottlerocket.injection
import au.com.gman.bottlerocket.imaging.BoundingBoxRescaler
import au.com.gman.bottlerocket.imaging.QrCodeDetector
import au.com.gman.bottlerocket.imaging.QrCodeTemplateMatcher
import au.com.gman.bottlerocket.interfaces.IBoundingBoxRescaler
import au.com.gman.bottlerocket.interfaces.IQrCodeDetector
import au.com.gman.bottlerocket.interfaces.IQrCodeTemplateMatcher
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class ImagingModule {

    @Binds
    abstract fun bindTemplateMapper(
        templateMapper: QrCodeTemplateMatcher
    ) : IQrCodeTemplateMatcher

    @Binds
    abstract fun bindQrCodeDetector(
        qrCodeDetector: QrCodeDetector
    ) : IQrCodeDetector

    @Binds
    abstract fun bindTemplateRescaler(
        pageTemplateRescaler: BoundingBoxRescaler
    ) : IBoundingBoxRescaler

}