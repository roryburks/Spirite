package rb.glow.gle

import rb.glow.CapMethod
import rb.glow.JoinMethod
import rb.glow.exceptions.GLEException
import rb.glow.gl.*
import rb.glow.glu.IPolygonTesselator
import rb.glow.glu.MatrixBuilder.F.orthagonalProjectionMatrix
import rb.glow.glu.MatrixBuilder.F.wrapTransform
import rb.vectrix.linear.ITransform
import rb.vectrix.linear.ITransformF
import rb.vectrix.linear.ImmutableTransformF
import rb.vectrix.linear.Vec3f
import rb.vectrix.mathUtil.f

class GlBindFramebufferException(msg: String) : GLEException(msg)

interface IGLEngine
{
    val tesselator : IPolygonTesselator
    val converter: IImageConverter

    val width : Int
    val height : Int

    var target: IGLTexture?
    fun setTarget( img: GLImage?)

    val gl : IGL
    fun runOnGLThread( run: ()->Unit)
    fun runInGLContext(run: ()->Unit)


    fun applyPassProgram(
            programCall: IGlProgramCall,
            params: GLParameters,
            trans: ITransform?,
            x1: Float, y1: Float, x2: Float, y2: Float,
            u1: Float = 0f, v1: Float = 0f, u2: Float = 1f, v2: Float = 1f)

    fun applyComplexLineProgram(
            xPoints: List<Float>, yPoints: List<Float>, numPoints: Int,
            cap: CapMethod, join: JoinMethod, loop: Boolean, lineWidth: Float,
            color: Vec3f, alpha: Float,
            params: GLParameters, trans: ITransformF?)

    fun applyPolyProgram(
            programCall: IGlProgramCall,
            xPoints: List<Float>,
            yPoints: List<Float>,
            numPoints: Int,
            polyType: PolyType,
            params: GLParameters,
            trans : ITransformF?)
    fun applyPrimitiveProgram(
            programCall: IGlProgramCall,
            primitive: IGLPrimitive,
            params: GLParameters,
            trans: ITransformF?)


    enum class BlendMethod(
            internal val sourceFactor: Int,
            internal val destFactor: Int,
            internal val formula: Int) {
        SRC_OVER(GLC.ONE, GLC.ONE_MINUS_SRC_ALPHA, GLC.FUNC_ADD),
        SOURCE(GLC.ONE, GLC.ZERO, GLC.FUNC_ADD),
        MAX(GLC.ONE, GLC.ONE, GLC.MAX),
        DEST_OVER(GLC.SRC_ALPHA, GLC.ONE_MINUS_SRC_ALPHA, GLC.FUNC_ADD),
        SRC (GLC.ONE, GLC.ZERO, GLC.FUNC_ADD),
    }
}

class GLEngine(
        override val tesselator: IPolygonTesselator,
        shaderLoader: IGLShaderLoader,
        override val converter: IImageConverter,
        private val _context: IGLContext)
    : IGLEngine
{
    private val programs = shaderLoader.initShaderPrograms()

    //private val dbo : IGLRenderbuffer by lazy { gl.genRenderbuffer() }
    private lateinit var fbo : IGLFramebuffer

    override val gl get() = _context.glGetter.invoke()

    override var width : Int = 1 ; private set
    override var height : Int = 1 ; private set

    override var target: IGLTexture? = null
        set(value) {
            
            if( field != value) {
                // Delete old Framebuffer
                if( field != null)
                    gl.deleteFramebuffer(fbo)

                if( value == null) {
                    gl.bindFrameBuffer(GLC.FRAMEBUFFER, null)
                    field = value
                    width = 1
                    height = 1
                }
                else {
                    fbo = gl.genFramebuffer() ?: return
                    gl.bindFrameBuffer(GLC.FRAMEBUFFER, fbo)

                    field = value
                    // I don't remember where I picked this up but I don't think it's working
//                    gl.bindRenderbuffer(GLC.RENDERBUFFER, dbo)
//                    gl.renderbufferStorage(GLC.RENDERBUFFER, GLC.DEPTH_COMPONENT16, 1, 1)
//                    gl.framebufferRenderbuffer(GLC.FRAMEBUFFER, GLC.DEPTH_ATTACHMENT, GLC.RENDERBUFFER, dbo)

                    // Attach Texture to FBO
                    gl.framebufferTexture2D( GLC.FRAMEBUFFER, GLC.COLOR_ATTACHMENT0, GLC.TEXTURE_2D, value, 0)

                    val status = gl.checkFramebufferStatus(GLC.FRAMEBUFFER)
                    when(status) {
                        GLC.FRAMEBUFFER_COMPLETE -> {}
                        else -> throw GlBindFramebufferException("Failed to bind Framebuffer: $status")
                    }
                }
            }
        }

    override fun setTarget(img: GLImage?) {
        if (img == null) {
            target = null
        } else {
            
            target = img.tex
            gl.viewport(0, 0, img.width, img.height)
        }
    }

    override fun runOnGLThread( run: () -> Unit) = _context.runOnGLThread(run)

    override fun runInGLContext(run: () -> Unit) = _context.runInGLContext(run)

    // region Exposed Rendering Methods

    override fun applyPassProgram(
            programCall: IGlProgramCall,
            params: GLParameters,
            trans: ITransform?,
            x1: Float, y1: Float, x2: Float, y2: Float,
            u1: Float, v1: Float, u2: Float, v2: Float)
    {
        val iParams = mutableListOf<GLUniform>()
        loadUniversalUniforms(params, iParams, trans)

        val preparedPrimitive = GLPrimitive(
                floatArrayOf(
                        // x  y   u   v
                        x1, y1, u1, v1,
                        x2, y1, u2, v1,
                        x1, y2, u1, v2,
                        x2, y2, u2, v2
                ), intArrayOf(2, 2), GLC.TRIANGLE_STRIP, intArrayOf(4)).prepare(gl)
        applyProgram( programCall, params, iParams, preparedPrimitive)
        preparedPrimitive.flush()
    }

    /**
     * Draws a complex line that transforms the line description into a geometric
     * shape by combining assorted primitive renders to create the specified
     * join/cap methods.
     *
     * @param xPoints	Array containing the xi coordinates.
     * @param yPoints 	Array containing the xi coordinates.
     * @param numPoints	Number of drawPoints to use for the render.
     * @param cap	How to draw the end-drawPoints.
     * @param join	How to draw the line joints.
     * @param loop	Whether or not to close the loop by joining the two end drawPoints
     * 	together (cap is ignored if this is true because the curve has no end drawPoints)
     * @param lineWidth    Width of the line.
     * @param color     Color of the line
     * @param alpha     Alpha of the line
     * @param params	GLParameters describing the GL Attributes to use
     * @param trans		ITransformF to apply to the rendering.
     */
    override fun applyComplexLineProgram(
            xPoints: List<Float>, yPoints: List<Float>, numPoints: Int,
            cap: CapMethod, join: JoinMethod, loop: Boolean, lineWidth: Float,
            color: Vec3f, alpha: Float,
            params: GLParameters, trans: ITransformF?)
    {
        
        if( xPoints.size < 2) return

        val size = numPoints + if(loop) 3 else 2
        val data = FloatArray(2*size)
        for( i in 1..numPoints) {
            data[i*2] = xPoints[i-1]
            data[i*2+1] = yPoints[i-1]
        }
        if (loop) {
            data[0] = xPoints[numPoints - 1]
            data[1] = yPoints[numPoints - 1]
            data[2 * (numPoints + 1)] = xPoints[0]
            data[2 * (numPoints + 1) + 1] = yPoints[0]
            if (numPoints > 2) {
                data[2 * (numPoints + 2)] = xPoints[1]
                data[2 * (numPoints + 2) + 1] = yPoints[1]
            }
        } else {
            data[0] = xPoints[0]
            data[1] = yPoints[0]
            data[2 * (numPoints + 1)] = xPoints[numPoints - 1]
            data[2 * (numPoints + 1) + 1] = yPoints[numPoints - 1]
        }

        val iParams = mutableListOf<GLUniform>()
        loadUniversalUniforms(params, iParams, trans, true)

        if( true /* Shaderversion 330 */) {
            val prim = GLPrimitive(
                    data,
                    intArrayOf(2),
                    GLC.LINE_STRIP_ADJACENCY,
                    intArrayOf(size)).prepare(gl)

            gl.enable(GLC.MULTISAMPLE)
            applyProgram(LineRenderCall(join, lineWidth, color, alpha), params, iParams, prim)
            gl.disable(GLC.MULTISAMPLE)

            prim.flush()
        }else {

        }
    }

    override fun applyPolyProgram(
            programCall: IGlProgramCall,
            xPoints: List<Float>,
            yPoints: List<Float>,
            numPoints: Int,
            polyType: PolyType,
            params: GLParameters,
            trans : ITransformF?)
    {
        val iParams = mutableListOf<GLUniform>()
        loadUniversalUniforms( params, iParams, trans)

        val data = FloatArray(2*numPoints)
        xPoints.forEachIndexed { i, x -> data[i*2] = x }
        yPoints.forEachIndexed { i, y -> data[i*2+1] = y }
        val prim = GLPrimitive(data, intArrayOf(2), polyType.glConst, intArrayOf(numPoints)).prepare(gl)

        applyProgram( programCall, params, iParams, prim)
        prim.flush()
    }

    override fun applyPrimitiveProgram(
            programCall: IGlProgramCall,
            primitive: IGLPrimitive,
            params: GLParameters,
            trans: ITransformF?
    ) {
        val iParams = mutableListOf<GLUniform>()
        loadUniversalUniforms( params, iParams, trans)
        val preparedPrimitive = primitive.prepare(gl)
        applyProgram( programCall, params, iParams, preparedPrimitive)
        preparedPrimitive.flush()
    }

    // endregion

    // region Base Rendering

    private fun applyProgram(
            programCall: IGlProgramCall,
            params: GLParameters,
            internalParams: List<GLUniform>,
            preparedPrimitive: IPreparedPrimitive)
    {
        
        val w = params.width
        val h = params.heigth

        val clipRect = params.clipRect
        when( clipRect) {
            null -> gl.viewport( 0, 0, w, h)
            else -> gl.viewport(clipRect.x1i, clipRect.y1i, clipRect.wi, clipRect.hi)
        }

        val program = programs[programCall.programKey] !!
        gl.useProgram(program)

        // Bind Attribute Streams
        preparedPrimitive.use()

        // Bind Texture
        val tex1 = params.texture1
        val tex2 = params.texture2
        if( tex1 != null) {
            gl.activeTexture(GLC.TEXTURE0)

            gl.bindTexture(GLC.TEXTURE_2D, tex1.tex)
            gl.enable(GLC.TEXTURE_2D)
            gl.uniform1i( gl.getUniformLocation(program, "u_texture"), 0)
        }
        if( tex2 != null) {
            gl.activeTexture(GLC.TEXTURE1)

            gl.bindTexture(GLC.TEXTURE_2D, tex2.tex)
            gl.enable(GLC.TEXTURE_2D)
            gl.uniform1i( gl.getUniformLocation(program, "u_texture2"), 0)
        }

        // Bind Uniforms
        programCall.uniforms?.forEach { it.apply(gl, program) }
        internalParams.forEach { it.apply(gl, program) }

        // Set Blend Mode
        if( params.useBlendMode) {
            gl.enable( GLC.BLEND)
            when( params.useDefaultBlendMode) {
                true -> {
                    val blendMethod = programCall.method
                    gl.blendFunc(blendMethod.sourceFactor,blendMethod.destFactor)
                    gl.blendEquation(blendMethod.formula)
                }
                else -> {
                    gl.blendFuncSeparate(params.bm_sfc, params.bm_dfc, params.bm_sfa, params.bm_dfa)
                    gl.blendEquationSeparate(params.bm_fc, params.bm_fa)
                }
            }
        }

        if( programCall.lineSmoothing) {
            gl.enable(GLC.LINE_SMOOTH)
            gl.enable(GLC.BLEND)
            gl.depthMask(false)
            gl.lineWidth(1f)
        }

        // Draw
        preparedPrimitive.draw()

        // Cleanup
        gl.disable(GLC.BLEND)
        gl.disable(GLC.LINE_SMOOTH)
        gl.depthMask(true)
        gl.disable(GLC.TEXTURE_2D)
        gl.useProgram(null)
        preparedPrimitive.unuse()
    }

    private fun loadUniversalUniforms(
            params: GLParameters,
            internalParams: MutableList<GLUniform>,
            trans: ITransform?,
            separateWorldTransfom: Boolean = false)
    {
        // Construct flags
        val flags =
                (if( params.premultiplied) 1 else 0) +
                        ((if( params.texture1?.premultiplied == true) 1 else 0) shl 1)

        internalParams.add(GLUniform1i("u_flags", flags))


        // Construct Projection Matrix
        val x1: Float
        val y1: Float
        val x2: Float
        val y2: Float

        val clipRect = params.clipRect
        if (clipRect == null) {
            x1 = 0f; x2 = params.width + 0f
            y1 = 0f; y2 = params.heigth + 0f
        } else {
            x1 = clipRect.x1i.f
            x2 = clipRect.x2i.f
            y1 = clipRect.y1i.f
            y2 = clipRect.y2i.f
        }

        var perspective = orthagonalProjectionMatrix(
                x1, x2, if (params.flip) y2 else y1, if (params.flip) y1 else y2, -1f, 1f)


        if( separateWorldTransfom) {
            internalParams.add(GLUniformMatrix4fv("perspectiveMatrix", perspective.transpose))
            internalParams.add(GLUniformMatrix4fv("worldMatrix", wrapTransform(trans
                    ?: ImmutableTransformF.Identity).transpose))
        }
        else {
            trans?.also {x  -> perspective = wrapTransform(x) * perspective }
            perspective = perspective.transpose
            internalParams.add(GLUniformMatrix4fv("perspectiveMatrix", perspective))
        }
    }

    // endregion
}


