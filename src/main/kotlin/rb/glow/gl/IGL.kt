package rb.glow.gl

/***
 * IGL is a wrapper interface for OpenGL contexts.  Note: this is still a fairly low-level wrapper and implies manual memory
 * management inherent in OpenGL, so should itself be wrapped in a Graphics Engine and shielded from the use sources.
 */
interface IGL {
    val tracker: IGLImageTracker?

    val preferredShaderVersion: GlShaderVersion
    val supportedShaderVersions: Set<GlShaderVersion>

    fun clearColor(red: Float, green: Float, blue: Float, alpha: Float, mask: Int)

    fun viewport( x: Int, y: Int, w: Int, h: Int)
    fun enable( cap: Int)
    fun disable( cap: Int)

    fun getError() : Int


    // Shader Stuff
    fun createShader( type: Int) : IGLShader?
    fun deleteShader( shader: IGLShader)
    fun shaderSource(shader: IGLShader, source: String)
    fun compileShader( shader: IGLShader)
    fun shaderCompiledSuccessfully(shader: IGLShader) : Boolean
    fun getShaderInfoLog( shader: IGLShader) : String?

    // Program Stuff
    fun createProgram() : IGLProgram?
    fun deleteProgram( program: IGLProgram)
    fun useProgram( program: IGLProgram?)
    fun attachShader(program: IGLProgram, shader: IGLShader)
    fun detatchShader(program: IGLProgram, shader: IGLShader)
    fun linkProgram( program: IGLProgram)
    fun programLinkedSuccessfully(program: IGLProgram) : Boolean
    fun getProgramInfoLog( program: IGLProgram) : String?

    // Texture Stuff
    fun createTexture() : IGLTexture?
    fun deleteTexture( texture: IGLTexture)
    fun bindTexture( target: Int, texture: IGLTexture?)
    fun activeTexture(texture: Int)
    fun texParameteri( target: Int, pname: Int, param: Int)
    fun texImage2D(target: Int, level: Int, internalformat: Int, format: Int, type: Int, source: ITextureSource)
    fun copyTexImage2D(target: Int, level: Int, internalformat: Int, x: Int, y: Int, width: Int, height: Int, border: Int)

    fun createBlankTextureSource(width: Int, height: Int) : ITextureSource

    // Uniform stuff
    fun getUniformLocation(program: IGLProgram, name: String) : IGLUniformLocation?
    fun uniform1f(location: IGLUniformLocation?, x: Float)
    fun uniform2f(location: IGLUniformLocation?, x: Float, y: Float)
    fun uniform3f(location: IGLUniformLocation?, x: Float, y: Float, z: Float)
    fun uniform4f(location: IGLUniformLocation?, x: Float, y: Float, z: Float, w: Float)
    fun uniform1i(location: IGLUniformLocation?, x: Int)
    fun uniform2i(location: IGLUniformLocation?, x: Int, y: Int)
    fun uniform3i(location: IGLUniformLocation?, x: Int, y: Int, z: Int)
    fun uniform4i(location: IGLUniformLocation?, x: Int, y: Int, z: Int, w: Int)
    fun uniformMatrix4fv(location: IGLUniformLocation?, data: IFloat32Source)
    fun uniform1iv(location: IGLUniformLocation?, data: IInt32Source)

    // Attribute Buffer Stuff
    fun getAttribLocation(program: IGLProgram, name: String) : Int
    fun createBuffer() : IGLBuffer?
    fun deleteBuffer( buffer: IGLBuffer)
    fun bindBuffer( target: Int, buffer: IGLBuffer?)
    fun bufferData(target: Int, data : IArraySource, usage: Int)
    fun enableVertexAttribArray( index: Int)
    fun disableVertexAttribArray( index: Int)
    fun vertexAttribPointer( index: Int, size: Int, type: Int, normalized: Boolean, stride: Int, offset: Int)

    // Framebuffers
    fun genFramebuffer(): IGLFramebuffer?
    fun deleteFramebuffer( buffer: IGLFramebuffer)
    fun bindFrameBuffer( target: Int, buffer: IGLFramebuffer?)
    fun framebufferTexture2D(target: Int, attachment: Int, textarget: Int, texture: IGLTexture, level: Int)
    fun checkFramebufferStatus( target: Int): Int
    fun genRenderbuffer() : IGLRenderbuffer?
    fun deleteRenderbuffer(renderBuffer: IGLRenderbuffer)
    fun bindRenderbuffer( target: Int, renderBuffer: IGLRenderbuffer)
    fun renderbufferStorage( target: Int, internalformat: Int, width: Int, height: Int)
    fun framebufferRenderbuffer( target: Int, attachment: Int, renderbuffertarget: Int, renderBuffer: IGLRenderbuffer)


    // Blend Modes
    fun blendFunc(sfactor: Int, dfactor:Int)
    fun blendEquation(mode:Int)
    fun blendFuncSeparate(srcRGB: Int, destRGB: Int, srcAlpha: Int, destAlpha: Int)
    fun blendEquationSeparate(modeRGB:Int, modeAlpha: Int)

    // Source Creation
    fun makeFloat32Source( size: Int) : IFloat32Source
    fun makeFloat32Source( array: FloatArray) : IFloat32Source
    fun makeInt32Source(size: Int): IInt32Source
    fun makeInt32Source(array: IntArray): IInt32Source

    // Draw
    fun drawArrays( mode: Int,first : Int, count: Int )

    fun readnPixels( x: Int, y: Int, w: Int, h: Int, format: Int, type: Int, n: Int, buffer: IArraySource)
    fun readPixels(x:Int, y:Int, w:Int, h:Int, format:Int, type:Int, buffer: IArraySource)

    fun depthMask( flag: Boolean)
    fun lineWidth( width: Float)
}

interface IGLShader{val gl: IGL;fun delete() = gl.deleteShader(this)}
interface IGLFramebuffer{val gl: IGL;fun delete() = gl.deleteFramebuffer(this)}
interface IGLRenderbuffer{val gl: IGL;fun delete() = gl.deleteRenderbuffer(this)}
interface IGLProgram{ val gl: IGL; fun delete() = gl.deleteProgram(this)}
interface IGLUniformLocation
interface IGLBuffer{ val gl: IGL; fun delete() = gl.deleteBuffer(this)}
interface IGLTexture{val gl: IGL; fun delete() = gl.deleteTexture(this)}

interface ITextureSource

interface IArraySource
interface IFloat32Source : IArraySource {
    operator fun get(index: Int) : Float
    operator fun set(index: Int, value : Float)
    val length: Int
}
interface IInt32Source : IArraySource {
    operator fun get(index: Int) : Int
    operator fun set(index: Int, value : Int)
    val length: Int
}

class GLResourcException(message: String?) : Exception(message)