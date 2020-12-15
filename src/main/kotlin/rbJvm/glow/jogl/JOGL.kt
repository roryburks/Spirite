package rbJvm.glow.jogl

import com.jogamp.opengl.GL
import com.jogamp.opengl.GL2
import rb.glow.gl.*
import rb.glow.gl.GlShaderVersion.GLSL_330
import rbJvm.glow.JvmImageTracker
import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.FloatBuffer
import java.nio.IntBuffer

/**
 * A wrapper to integrate JOGL's GL2 object into an IGL interface
 */
class JOGL(
    val gl : GL2
) : IGL {
    override val tracker: IGLImageTracker get() = JvmImageTracker

    override val preferredShaderVersion: GlShaderVersion get() = GLSL_330
    override val supportedShaderVersions: Set<GlShaderVersion> get() = setOf(GLSL_330)

    override fun clearColor(red: Float, green: Float, blue: Float, alpha: Float, mask: Int) {
        val color = FloatBuffer.wrap(floatArrayOf(red, green, blue, alpha))
        gl.glClearBufferfv(GLC.COLOR, 0, color)
    }

    override fun viewport(x: Int, y: Int, w: Int, h: Int) =
        gl.glViewport(x,y,w,h)
    override fun enable(cap: Int) =
        gl.glEnable(cap)
    override fun disable(cap: Int) =
        gl.glDisable(cap)

    override fun getError() =gl.glGetError()


    // region Shaders
    inner class JOGLShader( val shaderId: Int ) : IGLShader {
        override val gl: IGL get() = this@JOGL
    }

    override fun createShader(type: Int): IGLShader? {
        val shaderId = gl.glCreateShader(type)
        return if(shaderId == 0) null else JOGLShader(shaderId)
    }
    override fun deleteShader(shader: IGLShader) =
        gl.glDeleteShader((shader as JOGLShader).shaderId )
    override fun shaderSource(shader: IGLShader, source: String) {
        val shaderId = (shader as JOGLShader).shaderId
        val lines = arrayOf(source)
        val lengths = IntBuffer.wrap(intArrayOf(lines[0].length))
        gl.glShaderSource( shaderId, 1, lines, lengths)
    }
    override fun compileShader(shader: IGLShader) =
        gl.glCompileShader( (shader as JOGLShader).shaderId)
    override fun shaderCompiledSuccessfully(shader: IGLShader) : Boolean {
        val shaderId = (shader as JOGLShader).shaderId
        val status = IntBuffer.allocate(1)
        gl.glGetShaderiv(shaderId, GLC.COMPILE_STATUS, status)
        return status[0] == GLC.TRUE
    }
    override fun getShaderInfoLog(shader: IGLShader): String? {
        val shaderId = (shader as JOGLShader).shaderId
        val infoLogLength = IntBuffer.allocate(1)
        gl.glGetShaderiv(shaderId, GLC.INFO_LOG_LENGTH, infoLogLength)

        val bufferInfoLog = ByteBuffer.allocate( infoLogLength[0])
        gl.glGetShaderInfoLog( shaderId, infoLogLength[0], null, bufferInfoLog)

        val byteArray = ByteArray(infoLogLength[0])
        bufferInfoLog.get(byteArray)

        return String(byteArray)
    }

    inner class JOGLProgram( val programId: Int ) : IGLProgram {
        override val gl: IGL get() = this@JOGL
    }

    override fun createProgram(): IGLProgram? {
        val programId = gl.glCreateProgram()
        return if(programId == 0) null else JOGLProgram(programId)
    }
    override fun deleteProgram(program: IGLProgram)= gl.glDeleteProgram((program as JOGLProgram).programId)
    override fun useProgram(program: IGLProgram?) = gl.glUseProgram((program as? JOGLProgram)?.programId ?: 0)

    override fun attachShader(program: IGLProgram, shader: IGLShader) =
        gl.glAttachShader( (program as JOGLProgram).programId, (shader as JOGLShader).shaderId)
    override fun detatchShader(program: IGLProgram, shader: IGLShader) =
        gl.glDetachShader( (program as JOGLProgram).programId, (shader as JOGLShader).shaderId)

    override fun linkProgram(program: IGLProgram) = gl.glLinkProgram((program as JOGLProgram).programId)

    override fun programLinkedSuccessfully(program: IGLProgram): Boolean {
        val programId = (program as JOGLProgram).programId
        val status = IntBuffer.allocate(1)
        gl.glGetProgramiv(programId, GLC.LINK_STATUS, status)
        return status[0] == GL.GL_TRUE
    }

    override fun getProgramInfoLog(program: IGLProgram): String? {
        val programId = (program as JOGLProgram).programId
        val infoLogLength = IntBuffer.allocate(1)
        gl.glGetProgramiv(programId, GLC.INFO_LOG_LENGTH, infoLogLength)

        val bufferInfoLog = ByteBuffer.allocate( infoLogLength[0])
        gl.glGetProgramInfoLog( programId, infoLogLength[0], null, bufferInfoLog)

        val byteArray = ByteArray(infoLogLength[0])
        bufferInfoLog.get(byteArray)

        return String(byteArray)
    }
    // endregion

    // region Textures
    inner class JOGLTexture(val texId: Int) : IGLTexture {
        override val gl: IGL get() = this@JOGL
    }

    override fun createTexture(): IGLTexture? {
        val result = IntArray(1)
        gl.glGenTextures(1, result, 0)
        if( result[0] == 0){
            JOGLProvider.context.makeCurrent()
            gl.glGenTextures(1, result, 0)
        }
        return JOGLTexture(result[0])
    }

    override fun deleteTexture(texture: IGLTexture) =
        gl.glDeleteTextures( 1, intArrayOf((texture as JOGLTexture).texId), 0)
    override fun bindTexture(target: Int, texture: IGLTexture?) =
        gl.glBindTexture( target, (texture as JOGLTexture).texId)
    override fun activeTexture(texture: Int) =
        gl.glActiveTexture( texture)
    override fun texParameteri(target: Int, pname: Int, param: Int) =
        gl.glTexParameteri(target, pname, param)

    class JOGLTextureSource(
        val width: Int,
        val height: Int,
        val buffer: Buffer
    ) : ITextureSource

    override fun texImage2D(target: Int, level: Int, internalformat: Int, format: Int, type: Int, source: ITextureSource) {
        when( source) {
            is JOGLBlankTexture -> {
                val buff = ByteBuffer.allocate(source.width * source.height * 4)
                gl.glTexImage2D(target, level, internalformat, source.width, source.height, 0, format, type, buff)
            }
            is JOGLTextureSource ->
                gl.glTexImage2D( target, level, internalformat, source.width, source.height, 0, format, type, source.buffer)
        }
    }

    override fun copyTexImage2D(target: Int, level: Int, internalformat: Int, x: Int, y: Int, width: Int, height: Int, border: Int) =
        gl.glCopyTexImage2D( target, level, internalformat, x, y, width, height, border)

    inner class JOGLBlankTexture( val width: Int, val height: Int) : ITextureSource
    override fun createBlankTextureSource(width: Int, height: Int): ITextureSource = JOGLBlankTexture(width, height)
    // endregion

    // region Uniforms
    class JOGLUniformLocation( val locId: Int) : IGLUniformLocation

    override fun getUniformLocation(program: IGLProgram, name: String): IGLUniformLocation? =
        JOGLUniformLocation(gl.glGetUniformLocation((program as JOGLProgram).programId, name))

    override fun uniform1f(location: IGLUniformLocation?, x: Float) =
        gl.glUniform1f((location as JOGLUniformLocation).locId, x)
    override fun uniform2f(location: IGLUniformLocation?, x: Float, y: Float) =
        gl.glUniform2f((location as JOGLUniformLocation).locId, x, y)
    override fun uniform3f(location: IGLUniformLocation?, x: Float, y: Float, z: Float) =
        gl.glUniform3f((location as JOGLUniformLocation).locId, x, y, z)
    override fun uniform4f(location: IGLUniformLocation?, x: Float, y: Float, z: Float, w: Float) =
        gl.glUniform4f((location as JOGLUniformLocation).locId, x, y, z, w)

    override fun uniform1i(location: IGLUniformLocation?, x: Int) =
        gl.glUniform1i((location as JOGLUniformLocation).locId, x)
    override fun uniform2i(location: IGLUniformLocation?, x: Int, y: Int) =
        gl.glUniform2i((location as JOGLUniformLocation).locId, x, y)
    override fun uniform3i(location: IGLUniformLocation?, x: Int, y: Int, z: Int) =
        gl.glUniform3i((location as JOGLUniformLocation).locId, x, y, z)
    override fun uniform4i(location: IGLUniformLocation?, x: Int, y: Int, z: Int, w: Int) =
        gl.glUniform4i((location as JOGLUniformLocation).locId, x, y, z, w)

    override fun uniformMatrix4fv(location: IGLUniformLocation?, data: IFloat32Source) =
        gl.glUniformMatrix4fv((location as JOGLUniformLocation).locId, 1, false, (data as JOGLFloat32Source).data)

    override fun uniform1iv(location: IGLUniformLocation?, data: IInt32Source) =
        gl.glUniform1iv((location as JOGLUniformLocation).locId, data.length, (data as JOGLInt32Source).data)



    override fun getAttribLocation(program: IGLProgram, name: String): Int =
        gl.glGetAttribLocation((program as JOGLProgram).programId, name)
    // endregion

    // region Buffers
    inner class JOGLBuffer(val bufferId : Int) : IGLBuffer {
        override val gl: IGL get() = this@JOGL
    }

    override fun createBuffer(): IGLBuffer? {
        val result = IntBuffer.allocate(1)
        gl.glGenBuffers(1, result)
        return JOGLBuffer(result[0])
    }

    override fun deleteBuffer(buffer: IGLBuffer) =
        gl.glDeleteBuffers(1, intArrayOf((buffer as JOGLBuffer).bufferId), 0)
    override fun bindBuffer(target: Int, buffer: IGLBuffer?) =
        gl.glBindBuffer(target, (buffer as? JOGLBuffer)?.bufferId ?: 0)

    override fun bufferData(target: Int, data: IArraySource, usage: Int) {
        if( data is JOGLFloat32Source)
            gl.glBufferData(target, data.data.capacity()*4L, data.data, usage)
        // TODO : Other sources
    }

    override fun enableVertexAttribArray(index: Int) = gl.glEnableVertexAttribArray(index)
    override fun disableVertexAttribArray(index: Int) = gl.glDisableVertexAttribArray(index)

    override fun vertexAttribPointer(index: Int, size: Int, type: Int, normalized: Boolean, stride: Int, offset: Int) =
        gl.glVertexAttribPointer(index, size, type, normalized, stride, offset.toLong())
    // endregion

    // region Blending
    override fun blendFunc(sfactor: Int, dfactor: Int) =
        gl.glBlendFunc(sfactor, dfactor)
    override fun blendEquation(mode: Int) =
        gl.glBlendEquation(mode)

    override fun blendFuncSeparate(srcRGB: Int, destRGB: Int, srcAlpha: Int, destAlpha: Int) =
        gl.glBlendFuncSeparate(srcRGB,destRGB,srcAlpha,destAlpha)
    override fun blendEquationSeparate(modeRGB: Int, modeAlpha: Int) =
        gl.glBlendEquationSeparate( modeRGB, modeAlpha)
    // endregion

    // region Framebuffer
    inner class JOGLFramebuffer( val fboID : Int) : IGLFramebuffer {
        override val gl: IGL get() = this@JOGL
    }

    override fun genFramebuffer(): IGLFramebuffer {
        val result = IntArray(1)
        gl.glGenFramebuffers(1, result, 0)
        return JOGLFramebuffer( result[0])
    }
    override fun deleteFramebuffer(buffer: IGLFramebuffer) =
        gl.glDeleteFramebuffers( 1, intArrayOf((buffer as JOGLFramebuffer).fboID), 0)
    override fun bindFrameBuffer(target: Int, buffer: IGLFramebuffer?) =
        gl.glBindFramebuffer( target, (buffer as? JOGLFramebuffer)?.fboID ?: 0)
    override fun framebufferTexture2D(target: Int, attachment: Int, textarget: Int, texture: IGLTexture, level: Int) =
        gl.glFramebufferTexture2D( target, attachment, textarget, (texture as JOGLTexture).texId, level)
    override fun checkFramebufferStatus(target: Int) =
        gl.glCheckFramebufferStatus(target)

    inner class JOGLRenderbuffer(val dboId: Int) : IGLRenderbuffer {
        override val gl: IGL get() = this@JOGL
    }
    override fun genRenderbuffer(): IGLRenderbuffer {
        val result = IntArray(1)
        gl.glGenRenderbuffers(1, result, 0)
        return JOGLRenderbuffer(result[0])
    }

    override fun deleteRenderbuffer(renderBuffer: IGLRenderbuffer) =
        gl.glDeleteRenderbuffers(1, intArrayOf((renderBuffer as JOGLRenderbuffer).dboId), 0)
    override fun bindRenderbuffer(target: Int, renderBuffer: IGLRenderbuffer) =
        gl.glBindRenderbuffer(target, (renderBuffer as JOGLRenderbuffer).dboId)
    override fun renderbufferStorage(target: Int, internalformat: Int, width: Int, height: Int) =
        gl.glRenderbufferStorage(target, internalformat, width, height)
    override fun framebufferRenderbuffer(target: Int, attachment: Int, renderbuffertarget: Int, renderBuffer: IGLRenderbuffer) =
        gl.glFramebufferRenderbuffer(target, attachment, renderbuffertarget, (renderBuffer as JOGLRenderbuffer).dboId)
    // endregion

    // region Data Sources
    interface JOGLArraySource {
        val data: Buffer
    }

    class JOGLFloat32Source : IFloat32Source, JOGLArraySource {
        constructor( size: Int) {
            data = FloatBuffer.allocate(size)
            length = size
        }

        constructor( floatBuffer: FloatBuffer) {
            data = floatBuffer
            length = data.capacity()
        }

        override val data: FloatBuffer
        override fun get(index: Int): Float = data[index]
        override fun set(index: Int, value: Float) {data.put(index, value)}
        override val length : Int
    }
    override fun makeFloat32Source(size: Int): IFloat32Source = JOGLFloat32Source(size)
    fun makeFloat32Source(buffer: FloatBuffer): IFloat32Source = JOGLFloat32Source(buffer)
    override fun makeFloat32Source(array: FloatArray): IFloat32Source = JOGLFloat32Source(FloatBuffer.wrap(array))


    class JOGLInt32Source: IInt32Source, JOGLArraySource {
        constructor( size: Int) {
            data = IntBuffer.allocate(size)
            length = size
        }

        constructor( intBuffer: IntBuffer) {
            data = intBuffer
            length = data.capacity()
        }

        override val data: IntBuffer
        override fun get(index: Int): Int = data[index]
        override fun set(index: Int, value: Int) {data.put(index, value)}
        override val length : Int

    }
    override fun makeInt32Source(size: Int): IInt32Source = JOGLInt32Source(size)
    fun makeInt32Source(buffer: IntBuffer): IInt32Source = JOGLInt32Source(buffer)
    override fun makeInt32Source(array: IntArray): IInt32Source = JOGLInt32Source(IntBuffer.wrap(array))
    // endregion

    override fun drawArrays(mode: Int, first: Int, count: Int) =
        gl.glDrawArrays( mode, first, count)

    override fun readnPixels( x: Int, y: Int, w: Int, h: Int, format: Int, type: Int, n: Int, buffer: IArraySource){
        if( buffer is JOGLArraySource)
            gl.glReadnPixels( x, y, w, h, format, type, n, buffer.data)
    }
    override fun readPixels(x: Int, y: Int, w: Int, h: Int, format: Int, type: Int, buffer: IArraySource) {
        if( buffer is JOGLArraySource)
            gl.glReadPixels( x, y, w, h, format, type, buffer.data)
    }


    override fun depthMask(flag: Boolean) = gl.glDepthMask(flag)
    override fun lineWidth(width: Float) = gl.glLineWidth(width)
}

