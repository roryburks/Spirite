package sjunit.rb.glow.codec

import org.junit.jupiter.api.Test
import rb.glow.codec.GLImageDataCodecSvc
import rb.glow.codec.QoiCodec
import rb.glow.gl.GLC
import rb.glow.gl.GLImage
import rbJvm.glow.codec.NativeJavaPngImageCodec
import sjunit.testHelpers.runTest
import spirite.sguiHybrid.EngineLaunchpoint
import java.io.File
import java.io.FileOutputStream

class QoiCodecTests {
    private val _svc = QoiCodec()
    private val _outputDir = "E:/Bucket/sif"

    @Test fun encode_Simple(){
        runTest {

            val gle = EngineLaunchpoint.gle

            val glImage = GLImage(100, 100, gle, true)
            gle.setTarget(glImage)
            gle.gl.clearColor(0.1f, 0.8f, 0.9f, 1f, GLC.COLOR)

            val rawData = GLImageDataCodecSvc.export(glImage, gle)
            val exported = _svc.encode(rawData)

            val color = glImage.getColor(0, 0)
            println("${color.a} ${color.r} ${color.g} ${color.b}")

            val file = File("$_outputDir/qoiCodec_encode.qoi")
            FileOutputStream(file)
                .write(exported)
        }
    }
    @Test fun convertFromPng(){
        val inputFile = File("E:\\Bucket\\sif\\qoi_test_images\\dice.png")
        val outputFile = File("$_outputDir/qoiCodec_fromPng.qoi")

        val raw = inputFile.readBytes()
        val data = NativeJavaPngImageCodec().decode(raw)
        val asQoi = _svc.encode(data)

        FileOutputStream(outputFile).write(asQoi)
    }

    @Test fun dryRun(){
        val inputFile = File("E:\\Bucket\\sif\\qoi_test_images\\testcard_rgba.png")
        //val inputFile = File("$_outputDir/nativeJvmPngCodec_encode.png")
        val outputFile = File("$_outputDir/qoiCodec_dryRun.png")

        val raw = inputFile.readBytes()
        val data = NativeJavaPngImageCodec().decode(raw)
        val asPng = NativeJavaPngImageCodec().encode(data)

        FileOutputStream(outputFile).write(asPng)
    }

    @Test fun cycleFromPng(){
        val inputFile = File("E:\\Bucket\\sif\\qoi_test_images\\testcard_rgba.png")
        //val inputFile = File("$_outputDir/simplepng.png")
        val outputFile = File("$_outputDir/qoiCodec_roundTrip.png")

        val raw = inputFile.readBytes()
        val actualData = NativeJavaPngImageCodec().decode(raw)
        val intermediate1 = _svc.encode(actualData)
        val intermediate2 = _svc.decode(intermediate1)
        val outputPng = NativeJavaPngImageCodec().encode(intermediate2)

        FileOutputStream(outputFile).write(outputPng)
    }

    @Test fun convertToPng(){
        val inputFile = File("$_outputDir/qoiCodec_fromPng.qoi")
        val outputFile = File("$_outputDir/qoiCodec_converted.png")

        val raw = inputFile.readBytes()
        val data = _svc.decode(raw)
        val asPng = NativeJavaPngImageCodec().encode(data)

        FileOutputStream(outputFile).write(asPng)
    }
}