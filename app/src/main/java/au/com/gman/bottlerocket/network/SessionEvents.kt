package au.com.gman.bottlerocket.network

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionEvents @Inject constructor() {
    private val _loggedOut = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    val loggedOut =
        _loggedOut
            .asSharedFlow()

    fun notifyLoggedOut() {
        _loggedOut
            .tryEmit(Unit)
    }
}