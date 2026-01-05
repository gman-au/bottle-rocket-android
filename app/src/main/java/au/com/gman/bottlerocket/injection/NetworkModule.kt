package au.com.gman.bottlerocket.injection

import au.com.gman.bottlerocket.interfaces.IApiService
import au.com.gman.bottlerocket.network.ApiService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkModule {

    @Binds
    abstract fun bindApiService(
        apiService: ApiService
    ): IApiService
}