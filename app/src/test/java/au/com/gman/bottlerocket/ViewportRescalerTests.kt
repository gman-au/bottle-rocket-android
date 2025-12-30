package au.com.gman.bottlerocket

import au.com.gman.bottlerocket.domain.ScaleAndOffset
import au.com.gman.bottlerocket.scanning.ViewportRescaler
import org.junit.Test
import org.junit.Assert.*

class ViewportRescalerTests {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun test_landscape_aspect_ratio() {
        val context = TestContext()
        context.arrangeSourceSize(300F, 300F)
        context.arrangeTargetSize(600F, 600F)
        context.actCalculateOffset()
        context.assertAspectScale(1.0F, 1.0F)
        context.assertAspectOffset(0.0F, 0.0F)
    }

    private class TestContext {
        var sourceWidth: Float = 0F
        var sourceHeight: Float = 0F
        var targetWidth: Float = 0F
        var targetHeight: Float = 0F
        var rotationAngle: Int = 0

        val sut = ViewportRescaler()

        var result: ScaleAndOffset? = null

        fun arrangeSourceSize(width: Float, height: Float) {
            sourceWidth = width
            sourceHeight = height
        }

        fun arrangeTargetSize(width: Float, height: Float) {
            targetWidth = width
            targetHeight = height
        }

        fun actCalculateOffset() {
            result =
                sut
                    .calculateScalingFactorWithOffset(
                        sourceWidth,
                        sourceHeight,
                        targetWidth,
                        targetHeight,
                        rotationAngle
                    )
        }

        fun assertAspectScale(expectedX: Float, expectedY: Float) {
            assertEquals(expectedX, result?.scale?.x)
            assertEquals(expectedY, result?.scale?.y)
        }

        fun assertAspectOffset(expectedX: Float, expectedY: Float) {
            assertEquals(expectedX, result?.offset?.x)
            assertEquals(expectedY, result?.offset?.y)
        }
    }
}