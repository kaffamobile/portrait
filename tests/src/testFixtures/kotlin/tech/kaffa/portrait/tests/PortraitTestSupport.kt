package tech.kaffa.portrait.tests

import org.junit.Before
import tech.kaffa.portrait.Portrait

abstract class PortraitTestSupport {
    @Before
    fun clearPortraitCache() {
        Portrait.clearCache()
    }
}
