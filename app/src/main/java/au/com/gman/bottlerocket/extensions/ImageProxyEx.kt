package au.com.gman.bottlerocket.extensions

import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc

/** Convert ImageProxy YUV planes to OpenCV Mat (BGR) with rotation applied. */
fun ImageProxy.toMat(image: InputImage, rotationDegrees: Int): Mat? {
    try {
        val yPlane = planes[0]
        val uPlane = planes[1]
        val vPlane = planes[2]

        val yBuffer = yPlane.buffer
        val uBuffer = uPlane.buffer
        val vBuffer = vPlane.buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)

        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvMat = Mat(image.height + image.height / 2, image.width, CvType.CV_8UC1)
        yuvMat.put(0, 0, nv21)

        val bgrMat = Mat()
        Imgproc.cvtColor(yuvMat, bgrMat, Imgproc.COLOR_YUV2BGR_NV21)

        yuvMat.release()

        // Apply rotation to match MLKit coordinate space
        if (rotationDegrees == 0) {
            return bgrMat
        }

        val rotated = when (rotationDegrees) {
            90 -> {
                val result = Mat()
                Core.rotate(bgrMat, result, Core.ROTATE_90_CLOCKWISE)
                bgrMat.release()
                result
            }
            180 -> {
                val result = Mat()
                Core.rotate(bgrMat, result, Core.ROTATE_180)
                bgrMat.release()
                result
            }
            270 -> {
                val result = Mat()
                Core.rotate(bgrMat, result, Core.ROTATE_90_COUNTERCLOCKWISE)
                bgrMat.release()
                result
            }
            else -> bgrMat
        }

        return rotated
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }
}