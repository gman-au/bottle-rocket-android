import android.graphics.BitmapFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import au.com.gman.bottlerocket.qrCode.ContourEdgeDetector
import org.junit.Test
import org.junit.runner.RunWith
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Mat

@RunWith(AndroidJUnit4::class)
class QrEdgeDetectorTests {

    @Test
    fun testEdgeDetection() {
        OpenCVLoader.initDebug()

        val context = InstrumentationRegistry.getInstrumentation().context
        val inputStream = context.assets.open("PXL_SAMPLE_001.jpg")
        val bitmap = BitmapFactory.decodeStream(inputStream)

        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)

        val sut = ContourEdgeDetector()
        val edges = sut.detectEdges(mat)

        // Breakpoint here
        println("Detected ${edges?.size ?: 0} edge points")
    }
}