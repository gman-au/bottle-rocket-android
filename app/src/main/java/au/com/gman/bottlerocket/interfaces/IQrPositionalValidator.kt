package au.com.gman.bottlerocket.interfaces

import au.com.gman.bottlerocket.domain.RocketBoundingBox

interface IQrPositionalValidator {
    fun isBoxInsideBox(innerBox: RocketBoundingBox, outerBox: RocketBoundingBox): Boolean
}