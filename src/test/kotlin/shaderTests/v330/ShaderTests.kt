package shaderTests.v330

import spirite.base.graphics.gl.*
import spirite.base.graphics.gl.shader.GL330ShaderLoader
import spirite.base.util.f
import spirite.base.util.glu.GLC
import spirite.base.util.linear.Mat4
import spirite.base.util.linear.MatrixBuilder
import spirite.base.util.linear.toIFloat32Source
import spirite.hybrid.Hybrid
import spirite.pc.JOGL.JOGL.JOGLUniformLocation
import spirite.pc.resources.JClassScriptService
import kotlin.test.assertNotEquals

object ShaderTests
{
    val root = "shaders/330"
    private val GLOBAL = "#GLOBAL"

    val gl = Hybrid.gl
    private val gl330Loader = GL330ShaderLoader(gl, JClassScriptService())

    fun loadProgram(vert: String?, geom: String?, frag: String?) = gl330Loader.loadProgram(vert, geom, frag)

    fun bindTexture( texture: IGLTexture?) {
        if( texture == null) {
            gl.bindFrameBuffer(GLC.FRAMEBUFFER, null)
        }
        else {
            val fbo = gl.genFramebuffer()
            gl.bindFrameBuffer(GLC.FRAMEBUFFER, fbo)

            val dbo = gl.genRenderbuffer()
            gl.bindRenderbuffer(GLC.RENDERBUFFER, dbo)
            gl.renderbufferStorage(GLC.RENDERBUFFER, GLC.DEPTH_COMPONENT16, 1, 1)
            gl.framebufferRenderbuffer(GLC.FRAMEBUFFER, GLC.DEPTH_ATTACHMENT, GLC.RENDERBUFFER, dbo)

            gl.framebufferTexture2D(GLC.FRAMEBUFFER, GLC.COLOR_ATTACHMENT0, GLC.TEXTURE_2D, texture, 0)

            val status = gl.checkFramebufferStatus(GLC.FRAMEBUFFER)
            when(status) {
                GLC.FRAMEBUFFER_COMPLETE -> {}
                else -> throw Exception("Failed to bind Framebuffer: $status") }
        }
    }

    fun addPerpective(program: IGLProgram, x1: Float, x2: Float, y1: Float, y2: Float) {

        val mat = Mat4(MatrixBuilder.orthagonalProjectionMatrix(x1, x2, y1, y2, -1f, 1f)).transpose()

        gl.uniformMatrix4fv(getAndVerifyLocation(program,"perspectiveMatrix"), mat.toIFloat32Source(gl))
        gl.uniform1i(getAndVerifyLocation(program,"u_flags"), 0)
    }

    fun getAndVerifyLocation(program: IGLProgram, name: String) : IGLUniformLocation? {
        return gl.getUniformLocation(program, name)
                .also { assertNotEquals(-1, (it as JOGLUniformLocation).locId) }
    }

    internal fun drawPolyProgram(prim : PreparedPrimitive, program: IGLProgram, img: GLImage, uniformLoader: (IGL)->Unit)
    {
        ShaderTests.bindTexture(img.tex)
        gl.useProgram(program)
        gl.viewport( 0, 0, img.width, img.height)
        ShaderTests.addPerpective(program, 0f, img.width.f, 0f, img.height.f)

        uniformLoader(gl)

        // act
        prim.use()
        prim.draw()
        prim.unuse()
        gl.useProgram(null)
    }

    internal fun drawPolyProgramWithTexture(prim : PreparedPrimitive, program: IGLProgram, img: GLImage, tex: GLImage, uniformLoader: (IGL)->Unit)
    {
        ShaderTests.bindTexture(img.tex)
        gl.useProgram(program)
        gl.viewport( 0, 0, img.width, img.height)
        uniformLoader(gl)
        ShaderTests.addPerpective(program, 0f, img.width.f, 0f, img.height.f)

        gl.activeTexture(GLC.TEXTURE0)

        gl.bindTexture(GLC.TEXTURE_2D, tex.tex)
        gl.enable(GLC.TEXTURE_2D)
        gl.uniform1i( getAndVerifyLocation(program, "u_texture"), 0)


        // act
        prim.use()
        prim.draw()
        prim.unuse()
        gl.useProgram(null)
    }
}