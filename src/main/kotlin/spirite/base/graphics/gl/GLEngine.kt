package spirite.base.graphics.gl

import spirite.base.graphics.CapMethod
import spirite.base.graphics.JoinMethod
import spirite.base.graphics.gl.ProgramType.STROKE_V2_LINE_PASS
import spirite.base.graphics.gl.ProgramType.STROKE_V3_LINE_PASS
import spirite.base.graphics.gl.shader.GL330ShaderLoader
import spirite.base.resources.IScriptService
import spirite.base.util.glu.GLC
import rb.vectrix.linear.Mat4f
import spirite.base.util.linear.MatrixBuilder.orthagonalProjectionMatrix
import spirite.base.util.linear.MatrixBuilder.wrapTransformAs4x4
import rb.vectrix.linear.ITransformF
import rb.vectrix.linear.Vec3f
import rb.vectrix.linear.ImmutableTransformF
import spirite.hybrid.MDebug
import spirite.hybrid.MDebug.ErrorType
import spirite.pc.JOGL.JOGLProvider
import javax.swing.SwingUtilities

interface IGLEngine
{
    val width : Int
    val height : Int

    var target: IGLTexture?
    fun setTarget( img: GLImage?)

    val gl : IGL
    fun runOnGLThread( run: ()->Unit)
    fun runInGLContext(run: ()->Unit)


    fun applyPassProgram(
            programCall: ProgramCall,
            params: GLParameters,
            trans: ITransformF?,
            x1: Float, y1: Float, x2: Float, y2: Float)

    fun applyComplexLineProgram(
            xPoints: List<Float>, yPoints: List<Float>, numPoints: Int,
            cap: CapMethod, join: JoinMethod, loop: Boolean, lineWidth: Float,
            color: Vec3f, alpha: Float,
            params: GLParameters, trans: ITransformF?)

    fun applyPolyProgram(
            programCall: ProgramCall,
            xPoints: List<Float>,
            yPoints: List<Float>,
            numPoints: Int,
            polyType: PolyType,
            params: GLParameters,
            trans : ITransformF?)
    fun applyPrimitiveProgram(
            programCall: ProgramCall,
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
        private val glGetter: () -> IGL,
        scriptService: IScriptService
) : IGLEngine
{

    private val programs = initShaderPrograms(scriptService)

    //private val dbo : IGLRenderbuffer by lazy { gl.genRenderbuffer() }
    private lateinit var fbo : IGLFramebuffer

    override val gl get() = glGetter.invoke()

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
                    fbo = gl.genFramebuffer()
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
                        else -> MDebug.handleError(ErrorType.GL, "Failed to bind Framebuffer: $status") }
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

    override fun runOnGLThread( run: () -> Unit) {
        SwingUtilities.invokeLater{
            JOGLProvider.context.makeCurrent()
            run()
            JOGLProvider.context.release()
        }
    }

    override fun runInGLContext(run: () -> Unit) {
        JOGLProvider.context.makeCurrent()
        run()
        JOGLProvider.context.release()
    }

    // region Exposed Rendering Methods

    override fun applyPassProgram(
            programCall: ProgramCall,
            params: GLParameters,
            trans: ITransformF?,
            x1: Float, y1: Float, x2: Float, y2: Float)
    {
        val iParams = mutableListOf<GLUniform>()
        loadUniversalUniforms(params, iParams, trans)

        val preparedPrimitive = GLPrimitive(
                floatArrayOf(
                        // xi  yi   u   v
                        x1, y1, 0.0f, 0.0f,
                        x2, y1, 1.0f, 0.0f,
                        x1, y2, 0.0f, 1.0f,
                        x2, y2, 1.0f, 1.0f
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
     * @param numPoints	Number of points to use for the render.
     * @param cap	How to draw the end-points.
     * @param join	How to draw the line joints.
     * @param loop	Whether or not to close the loop by joining the two end points
     * 	together (cap is ignored if this is true because the curve has no end points)
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
            applyProgram(LineRenderCall( join, lineWidth, color, alpha), params, iParams, prim)
            gl.disable(GLC.MULTISAMPLE)

            prim.flush()
        }else {

        }
    }

    override fun applyPolyProgram(
            programCall: ProgramCall,
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
        val prim = GLPrimitive( data, intArrayOf(2), polyType.glConst, intArrayOf(numPoints)).prepare(gl)

        applyProgram( programCall, params, iParams, prim)
        prim.flush()
    }

    override fun applyPrimitiveProgram(
            programCall: ProgramCall,
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
            programCall: ProgramCall,
            params: GLParameters,
            internalParams: List<GLUniform>,
            preparedPrimitive: IPreparedPrimitive)
    {
        
        val w = params.width
        val h = params.heigth

        val clipRect = params.clipRect
        when( clipRect) {
            null -> gl.viewport( 0, 0, w, h)
            else -> gl.viewport(clipRect.x, clipRect.y, clipRect.width, clipRect.height)
        }

        val program = programs[programCall.programType.ordinal]
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
                    val blendMethod = programCall.programType.method
                    gl.blendFunc(blendMethod.sourceFactor,blendMethod.destFactor)
                    gl.blendEquation(blendMethod.formula)
                }
                else -> {
                    gl.blendFuncSeparate(params.bm_sfc, params.bm_dfc, params.bm_sfa, params.bm_dfa)
                    gl.blendEquationSeparate(params.bm_fc, params.bm_fa)
                }
            }
        }

        when( programCall.programType) {
            STROKE_V2_LINE_PASS,
            STROKE_V3_LINE_PASS -> {
                gl.enable(GLC.LINE_SMOOTH)
                gl.enable(GLC.BLEND)
                gl.depthMask(false)
                gl.lineWidth(1f)
            }
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
            trans: ITransformF?,
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
            x1 = clipRect.x + 0f
            x2 = clipRect.x + clipRect.width + 0f
            y1 = clipRect.y + 0f
            y2 = clipRect.y + clipRect.height + 0f
        }

        var perspective = Mat4f(orthagonalProjectionMatrix(
                x1, x2, if (params.flip) y2 else y1, if (params.flip) y1 else y2, -1f, 1f))


        if( separateWorldTransfom) {
            internalParams.add(GLUniformMatrix4fv("perspectiveMatrix", perspective.transpose))
            internalParams.add(GLUniformMatrix4fv("worldMatrix", Mat4f(wrapTransformAs4x4(trans
                    ?: ImmutableTransformF.Identity)).transpose))
        }
        else {
            trans?.also {x  -> perspective = Mat4f(wrapTransformAs4x4(x)) * perspective }
            perspective = perspective.transpose
            internalParams.add(GLUniformMatrix4fv("perspectiveMatrix", perspective))
        }
    }

    // endregion

    private fun initShaderPrograms(scriptService: IScriptService) : Array<IGLProgram> {
        return GL330ShaderLoader(gl, scriptService).initShaderPrograms()
    }
}


class GLEException(message: String) : Exception(message)