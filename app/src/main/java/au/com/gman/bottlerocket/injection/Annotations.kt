package au.com.gman.bottlerocket.injection

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class TheContourPointDetector

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class TheArtifactPointDetector