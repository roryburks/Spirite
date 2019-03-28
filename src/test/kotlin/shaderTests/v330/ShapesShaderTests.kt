//package shaderTests.v330
//
//import io.mockk.every
//import io.mockk.mockk
//import rb.vectrix.linear.ImmutableTransformF
//import rb.vectrix.linear.Mat4f
//import rb.vectrix.mathUtil.i
//import sjunit.TestConfig
//import spirite.base.graphics.gl.GLImage
//import spirite.base.graphics.gl.GLPrimitive
//import spirite.base.graphics.gl.IGLEngine
//import spirite.base.graphics.gl.PreparedPrimitive
//import spirite.base.util.glu.GLC
//import spirite.base.util.linear.MatrixBuilder
//import spirite.pc.toBufferedImage
//import java.io.File
//import javax.imageio.ImageIO
//import kotlin.test.assertEquals
//import org.junit.Test as test
//
//class ShapesShaderTests
//{
//    val mockGle = mockk<IGLEngine>()
//
//    val root get() = ShaderTests.root
//    val gl get() = ShaderTests.gl
//
//    init {
//        every { mockGle.getGl() } returns gl
//
//        every { mockGle.setTarget(any()) } answers { ShaderTests.bindTexture(firstArg<GLImage>().tex)}
//    }
//
//
//    @test fun PolyRender() {
//        // Arrange
//        val program = ShaderTests.loadProgram(
//                "${root}/shapes/poly_render.vert",
//                null,
//                "${root}/shapes/shape_render.frag")
//
//        val gli = GLImage(100, 100, mockGle)
//
//        val points = floatArrayOf(
//                25f, 25f,
//                75f, 25f,
//                25f, 75f,
//                75f, 75f
//        )
//
//        val pp = PreparedPrimitive( GLPrimitive(points, intArrayOf(2), GLC.TRIANGLE_STRIP, intArrayOf(4)), gl)
//
//        // act
//        ShaderTests.drawPolyProgram(pp, program, gli){gl ->
//            gl.uniform3f(ShaderTests.getAndVerifyLocation(program,"u_color"),1f, 0f, 0f)
//            gl.uniform1f(ShaderTests.getAndVerifyLocation(program,"u_alpha"), 1f)
//        }
//
//        // output
//        if( TestConfig.save) {
//            ImageIO.write(gli.toBufferedImage() , "png", File("${TestConfig.saveLocation}\\shaders\\Prim.png"))
//        }
//
//        // assert
//        assertEquals(0, gli.getARGB(0,0))
//        assertEquals(0xffff0000.i, gli.getARGB(50,50))
//        assertEquals(0, gli.getARGB(99,99))
//    }
//
//    @test fun LineRender() {
//        // Arrange
//        val program = ShaderTests.loadProgram(
//                "${root}/shapes/line_render.vert",
//                "${root}/shapes/line_render.geom",
//                "${root}/shapes/shape_render.frag")
//
//        val gli = GLImage(100, 100, mockGle)
//
//        val points = floatArrayOf(
//                25f, 25f, 25f, 25f,
//                75f, 25f,
//                25f, 75f,
//                75f, 75f, 75f,75f
//        )
//
//        val pp = PreparedPrimitive( GLPrimitive(points, intArrayOf(2), GLC.LINE_STRIP_ADJACENCY, intArrayOf(6)), gl)
//
//        // act
//        gl.enable(GLC.MULTISAMPLE)
//        ShaderTests.drawPolyProgram(pp, program, gli){gl ->
//            gl.uniform1i(ShaderTests.getAndVerifyLocation(program,"u_join"),1)
//            gl.uniform1f(ShaderTests.getAndVerifyLocation(program,"u_width"), 1.5f)
//            gl.uniform3f(ShaderTests.getAndVerifyLocation(program,"u_color"),1f, 0f, 0f)
//            gl.uniform1f(ShaderTests.getAndVerifyLocation(program,"u_alpha"), 1f)
//
//            val mat = Mat4f(MatrixBuilder.wrapTransformAs4x4(ImmutableTransformF.Identity)).transpose()
//            gl.uniformMatrix4fv(ShaderTests.getAndVerifyLocation(program, "worldMatrix"), mat.toIFloat32Source(gl))
//        }
//
//        // output
//        if( TestConfig.save) {
//            ImageIO.write(gli.toBufferedImage() , "png", File("${TestConfig.saveLocation}\\shaders\\Line.png"))
//        }
//
//        // assert
//        assertEquals(0, gli.getARGB(0,0))
//        assertEquals(0xffff0000.i, gli.getARGB(50,50))
//        assertEquals(0, gli.getARGB(99,99))
//    }
//
//}