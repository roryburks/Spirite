//package shaderTests.v330
//
//import io.mockk.every
//import io.mockk.mockk
//import rb.vectrix.mathUtil.f
//import sjunit.TestConfig
//import spirite.base.graphics.gl.GLImage
//import rb.glow.gle.GLPrimitive
//import spirite.base.graphics.gl.IGLEngine
//import rb.glow.gle.PreparedPrimitive
//import rb.glow.gl.GLC
//import spirite.pc.toBufferedImage
//import java.io.File
//import javax.imageio.ImageIO
//import org.junit.Test as test
//
//
//class FiltersShaderTests {
//    val mockGle = mockk<IGLEngine>()
//
//    val root get() = ShaderTests.root
//    val gl get() = ShaderTests.gl
//
//    init {
//        //every { mockGle.getGl() } returns gl
//
//        every { mockGle.setTarget(any()) } answers { ShaderTests.bindTexture(firstArg<GLImage>().tex)}
//    }
//
//    // NOTE: CreateBaseImage for Filters is essentially the Poly shader test from ShapesShaderTests
//    fun createBaseImage(w: Int, h: Int) : GLImage {
//
//        val program = ShaderTests.loadProgram(
//                "${root}/shapes/poly_render.vert",
//                null,
//                "${root}/shapes/shape_render.frag")
//
//        val gli = GLImage(w, h, mockGle)
//
//        val b = w / 4.0f
//        val points = floatArrayOf(
//                b, b,
//                w-b, b,
//                b, h-b,
//                w-b, h-b)
//
//        val pp = PreparedPrimitive( GLPrimitive(points, intArrayOf(2), GLC.TRIANGLE_STRIP, intArrayOf(4)), gl)
//
//        ShaderTests.drawPolyProgram(pp, program, gli){gl ->
//            gl.uniform3f(ShaderTests.getAndVerifyLocation(program,"u_color"),1f, 0f, 0f)
//            gl.uniform1f(ShaderTests.getAndVerifyLocation(program,"u_alpha"), 1f)
//        }
//
//        return  gli
//    }
//
//
//    @test fun ChangeColor() {
//        // Arrange
//
//        val gli = GLImage(100, 100, mockGle)
//        val base = createBaseImage(gli.width, gli.height)
//
//        val program = ShaderTests.loadProgram(
//                "${root}/pass.vert",
//                null,
//                "${root}/filters/pass_change_color.frag")
//
//        val points = floatArrayOf(
//                0f, 0f, 0f, 0f,
//                base.width.f, 0f, 1f, 0f,
//                0f, base.height.f, 0f, 1f,
//                base.width.f, base.height.f, 1f, 1f
//        )
//
//        val pp = PreparedPrimitive( GLPrimitive(points, intArrayOf(2,2), GLC.TRIANGLE_STRIP, intArrayOf(4)), gl)
//
//        // act
//        ShaderTests.drawPolyProgramWithTexture(pp, program, gli, base){gl ->
//            gl.uniform4f(ShaderTests.getAndVerifyLocation(program,"u_fromColor"),1f, 0f, 0f,1f)
//            gl.uniform4f(ShaderTests.getAndVerifyLocation(program,"u_toColor"), 0f, 1f, 0f,1f)
//            gl.uniform1i(ShaderTests.getAndVerifyLocation(program,"u_optionMask"), 1)
//        }
//
//        // output
//        if( TestConfig.save) {
//            ImageIO.write(gli.toBufferedImage() , "png", File("${TestConfig.saveLocation}\\shaders\\ChangeColor.png"))
//        }
//    }
//}