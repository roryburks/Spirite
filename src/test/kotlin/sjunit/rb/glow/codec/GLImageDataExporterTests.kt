package sjunit.rb.glow.codec

import org.junit.jupiter.api.Test
import rb.glow.gl.GLC
import rb.glow.gl.GLImage
import rb.glow.codec.GLImageDataCodecSvc
import rb.vectrix.mathUtil.floor
import sjunit.testHelpers.runTest
import spirite.sguiHybrid.EngineLaunchpoint
import kotlin.test.assertEquals

class GLImageDataExporterTests {
    private val _svc = GLImageDataCodecSvc

    @Test fun testConversion() {
        runTest {
            val gle = EngineLaunchpoint.gle

            val glImage = GLImage(100, 100, gle, true)
            gle.setTarget(glImage)
            gle.gl.clearColor(0.1f, 0.1f, 1f, 1f, GLC.COLOR)

            val rawData = _svc.export(glImage, gle)

            val expectedRG = (0.1f * 256).floor.toByte()
            rawData.raw.forEachIndexed { index, byte ->
                when( index % 4) {
                    0 -> assertEquals(255.toByte(), byte)
                    1 -> assertEquals(expectedRG, byte)
                    2 -> assertEquals(expectedRG, byte)
                    else -> assertEquals(255.toByte(), byte)
                }
            }
        }
    }

    @Test fun testCycle() {
        runTest {
            val gle = EngineLaunchpoint.gle

            val glImage = GLImage(100, 100, gle, true)
            gle.setTarget(glImage)
            gle.gl.clearColor(0.1f, 0.5f, 0.8f, 1f, GLC.COLOR)

            val rawData = _svc.export(glImage, gle)
            val glImage2 = _svc.import(rawData, gle)

            for (x in 0..100) {
                for (y in 0..100) {
                    val c1 = glImage.getARGB(x,y)
                    val c2 = glImage2.getARGB(x,y)
                    assertEquals(c1,c2)
                }
            }
        }
    }
}