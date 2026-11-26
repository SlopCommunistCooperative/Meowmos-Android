package io.github.slopcommunistcooperative.meowmos.data.model

import io.github.slopcommunistcooperative.meowmos.util.SettingsSerializer
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsTest {
    @Test fun oldSettingsKeepFollowingSystemColors() = runTest {
        val settings = SettingsSerializer.readFrom("""{"currentUser":"local"}""".byteInputStream())
        assertTrue(settings.dynamicColorEnabled)
        assertEquals("local", settings.currentUser)
    }

    @Test fun optingOutOfDynamicColorSurvivesReload() = runTest {
        val settings = Settings(currentUser = "local", appLockEnabled = true, dynamicColorEnabled = false)
        val serialized = ByteArrayOutputStream()
        SettingsSerializer.writeTo(settings, serialized)
        assertEquals(settings, SettingsSerializer.readFrom(serialized.toByteArray().inputStream()))
    }
}
