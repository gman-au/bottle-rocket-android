package au.com.gman.bottlerocket.injection
import au.com.gman.bottlerocket.file.FileIo
import au.com.gman.bottlerocket.interfaces.IFileIo
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class FileModule {

    @Binds
    abstract fun bindFileIo(
        fileIo: FileIo
    ) : IFileIo
}