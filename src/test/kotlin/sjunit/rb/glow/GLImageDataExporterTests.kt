package sjunit.rb.glow

import org.junit.jupiter.api.Test
import rb.glow.gl.GLC
import rb.glow.gl.GLImage
import rb.glow.gl.services.GLImageDataExporter
import rb.vectrix.mathUtil.floor
import rb.vectrix.mathUtil.round
import sjunit.testHelpers.runTest
import spirite.sguiHybrid.EngineLaunchpoint
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GLImageDataExporterTests {
    @Test fun testConversion() {
        runTest {
            val gle = EngineLaunchpoint.gle

            val glImage = GLImage(100, 100, gle, true)
            gle.setTarget(glImage)
            gle.gl.clearColor(0.1f, 0.1f, 1f, 1f, GLC.COLOR)

            val rawData = GLImageDataExporter.export(glImage, gle)

            val expectedRG = (0.1f * 256).floor.toByte()
            rawData.raw.forEachIndexed { index, byte ->
                when( index % 4) {
                    0 -> assertEquals(expectedRG, byte)
                    1 -> assertEquals(expectedRG, byte)
                    2 -> assertEquals(255.toByte(), byte)
                    else -> assertEquals(255.toByte(), byte)
                }
            }
        }
    }
}