package au.com.gman.bottlerocket
import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp
import org.opencv.android.OpenCVLoader

@HiltAndroidApp
class BottleRocketApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        OpenCVLoader.initDebug();
        // Your global initialization code here (e.g., crash reporting, shared network client)
        Log.d("MyApplication", "Application class created and onCreate called")
    }

    companion object {
        // You can add a static instance for easy access if needed
        const val USE_SMOOTHING = true
        const val USE_VALIDATION = true
    }
}