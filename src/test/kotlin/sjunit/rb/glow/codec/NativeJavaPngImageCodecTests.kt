package sjunit.rb.glow.codec

import org.junit.jupiter.api.Test
import rb.glow.gl.GLC
import rb.glow.gl.GLImage
import rb.glow.codec.GLImageDataCodecSvc
import rb.vectrix.mathUtil.floor
import rbJvm.glow.codec.NativeJavaPngImageCodec
import sjunit.testHelpers.runTest
import spirite.sguiHybrid.EngineLaunchpoint
import java.io.File
import java.io.FileOutputStream
import kotlin.test.assertEquals

class NativeJavaPngImageCodecTests {
    private val _svc = NativeJavaPngImageCodec()
    private val _outputDir = "E:/Bucket/sif"

    @Test fun testEncode() {
        runTest {
            val gle = EngineLaunchpoint.gle

            val glImage = GLImage(100, 100, gle, true)
            gle.setTarget(glImage)
            gle.gl.clearColor(0.1f, 0.8f, 0.9f, 1f, GLC.COLOR)

            val rawData = GLImageDataCodecSvc.export(glImage, gle)
            val exported = _svc.encode(rawData)

            val color = glImage.getColor(0, 0)
            println("${color.a} ${color.r} ${color.g} ${color.b}")

            val file = File("$_outputDir/nativeJvmPngCodec_encode.png")
            FileOutputStream(file)
                .write(exported)
        }
    }

    @Test fun testDecode() {
        runTest {
            val gle = EngineLaunchpoint.gle

            val file = File("$_outputDir/nativeJvmPngCodec_encode.png")
            val raw = file.readBytes()

            val data = _svc.decode(raw)
            val img = GLImageDataCodecSvc.import(data, gle)

            for (x in 0 until data.width) {
                for (y in 0 until data.height) {
                    val color = img.getColor(x, y)
//                    val a =  color.a
//                    val r = color.r
//                    val g = color.g
//                    val b = color.b
//                    println("$a 255 $r 25 $g 204 $b 229")
                    assertEquals(255, color.a)
                    assertEquals((0.1f*255).floor, color.r)
                    assertEquals((0.8f*255).floor, color.g)
                    assertEquals((0.9f*255).floor, color.b)
                    data.raw[y* data.width]

                }

            }
        }

    }
}