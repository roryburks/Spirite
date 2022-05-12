package sjunit.rb.glow.codec

import org.junit.jupiter.api.Test
import rb.glow.gl.GLC
import rb.glow.gl.GLImage
import rb.glow.codec.GLImageDataCodecSvc
import rbJvm.glow.codec.NativeJavaPngImageCodec
import sjunit.testHelpers.runTest
import spirite.sguiHybrid.EngineLaunchpoint
import java.io.File
import java.io.FileOutputStream

class NativeJavaPngImageCodecTests {
    private val _svc = NativeJavaPngImageCodec()
    private val _outputDir = "E:/Bucket/sif"

    @Test fun testEncode() {
        runTest {
            val gle = EngineLaunchpoint.gle

            val glImage = GLImage(100, 100, gle, true)
            gle.setTarget(glImage)
            gle.gl.clearColor(0.1f, 0.8f, 1f, 1f, GLC.COLOR)

            val rawData = GLImageDataCodecSvc.export(glImage, gle)
            val exported = _svc.encode(rawData)

            val file = File("$_outputDir/nativeJvmPngCodec_encode.png")
            FileOutputStream(file)
                .write(exported)
        }
    }

    @Test fun testDecode() {
        val gle = EngineLaunchpoint.gle

    }
}