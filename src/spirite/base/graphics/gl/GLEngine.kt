package spirite.base.graphics.gl

import com.hackoeur.jglm.Mat4
import spirite.base.graphics.gl.GLEngine.BlendMethod.*
import spirite.base.resources.IScriptService
import spirite.base.util.glu.GLC
import spirite.base.util.linear.MatrixBuilder.orthagonalProjectionMatrix
import spirite.base.util.linear.MatrixBuilder.wrapTransformAs4x4
import spirite.base.util.linear.Transform

class GLEngine(
        internal val gl: IGL,
        scriptService: IScriptService
) {

    val programs = initShaderPrograms(scriptService)

    val dbo : IGLRenderbuffer by lazy { gl.genRenderbuffer() }
    lateinit private var fbo : IGLFramebuffer

    var width : Int = 1 ; private set
    var height : Int = 1 ; private set


    var target: IGLTexture? = null
        get
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
                    gl.bindRenderbuffer(GLC.RENDERBUFFER, dbo)
                    gl.renderbufferStorage(GLC.RENDERBUFFER, GLC.DEPTH_COMPONENT16, 1, 1)
                    gl.framebufferRenderbuffer(GLC.FRAMEBUFFER, GLC.DEPTH_ATTACHMENT, GLC.RENDERBUFFER, dbo)

                    // Attach Texture to FBO
                    gl.framebufferTexture2D( GLC.FRAMEBUFFER, GLC.COLOR_ATTACHMENT0, GLC.TEXTURE_2D, value, 0)

                }
            }
        }

    fun setTarget(img: GLImage?) {
        if (img == null) {
            target = null
        } else {
            target = img.tex
            gl.viewport(0, 0, img.width, img.height)
            width = img.width
            height = img.height
        }
    }

    fun runOnGLThread( run: () -> Unit) {

    }

    // region Exposed Rendering Methods

    fun applyPolyProgram(
            program: ProgramType,
            xPoints: List<Float>,
            yPoints: List<Float>,
            numPoints: Int,
            polyType: PolyType,
            params: GLParameters,
            trans : Transform?
    ) {
        val iParams = InternalParams(params)
        addOrtho( iParams, trans)

        val data = gl.makeFloat32Source(2*numPoints)
        xPoints.forEachIndexed { i, x -> data[i*2] = x }
        yPoints.forEachIndexed { i, y -> data[i*2+1] = y }
        val prim = PreparedPrimitive(GLPrimitive( data, intArrayOf(2), polyType.glConst, intArrayOf(numPoints)), this)

        applyProgram( program, iParams, prim)
        prim.flush()
    }

    // endregion

    // region Base Rendering

    private inner class InternalParams(
            val baseParams: GLParameters
    ){
        val internalParams = mutableListOf<GLUniform>()
    }

    private fun applyProgram( programType: ProgramType, iParams: InternalParams, preparedPrimitive: PreparedPrimitive) {
        val params = iParams.baseParams
        val prim = preparedPrimitive.primative
        val w = params.width
        val h = params.heigth

        val clipRect = params.clipRect
        when( clipRect) {
            null -> gl.viewport( 0, 0, w, h)
            else -> gl.viewport(clipRect.x, clipRect.y, clipRect.width, clipRect.height)
        }

        val program = programs[programType.ordinal]
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
        params.uniforms?.forEach { it.apply(gl, program) }
        iParams.internalParams.forEach { it.apply(gl, program) }

        // Set Blend Mode
        if( params.useBlendMode) {
            gl.enable( GLC.BLEND)
            when( params.useDefaultBlendMode) {
                true -> {
                    val blendMethod = programType.method
                    gl.blendFunc(blendMethod.sourceFactor,blendMethod.destFactor)
                    gl.blendEquation(blendMethod.formula)
                }
                else -> {
                    gl.blendFuncSeparate(params.bm_sfc, params.bm_dfc, params.bm_sfa, params.bm_dfa)
                    gl.blendEquationSeparate(params.bm_fc, params.bm_fa)
                }
            }
        }

        if( program == ProgramType.STROKE_V2_LINE_PASS) {
            gl.enable(GLC.LINE_SMOOTH)
            gl.enable(GLC.BLEND)
            gl.depthMask(false)
            gl.lineWidth(1f)
        }

        // Draw
        var start = 0
        for( i in 0 until prim.primitiveLengths.size) {
            gl.drawArrays(prim.primitiveTypes[i], start, prim.primitiveLengths[i])
            start += prim.primitiveLengths[i]
        }

        // Cleanup
        gl.disable(GLC.BLEND)
        gl.disable(GLC.LINE_SMOOTH)
        gl.depthMask(true)
        gl.disable(GLC.TEXTURE_2D)
        gl.useProgram(null)
        preparedPrimitive.unuse()
    }

    private fun addOrtho( params: InternalParams, trans: Transform?) {
        val x1 : Float
        val y1 : Float
        val x2 : Float
        val y2 : Float
        val base = params.baseParams

        val clipRect = base.clipRect
        if( clipRect == null) {
            x1 = 0f; x2 = base.width + 0f
            y1 = 0f; y2 = base.heigth + 0f
        }
        else {
            x1 = clipRect.x + 0f
            x2 = clipRect.x + clipRect.width + 0f
            y1 = clipRect.y + 0f
            y2 = clipRect.y + clipRect.height + 0f
        }

        var matrix = Mat4(orthagonalProjectionMatrix(
                x1, x2,if( base.flip) y2 else y1, if( base.flip) y1 else y2, -1f, 1f))

        if( trans != null) {
            val matrix2 = Mat4( wrapTransformAs4x4(trans))
            matrix = matrix2.multiply(matrix)
        }

        matrix = matrix.transpose()
        params.internalParams.add(GLUniformMatrix4fv("perspectiveMatrix", matrix))
    }
    // endregion

    // region Shader Programs
    private enum class BlendMethod(
            val sourceFactor: Int,
            val destFactor: Int,
            val formula: Int) {
        SRC_OVER( GLC.ONE, GLC.ONE_MINUS_SRC_ALPHA, GLC.FUNC_ADD),
        SOURCE(GLC.ONE, GLC.ZERO, GLC.FUNC_ADD),
        MAX( GLC.ONE, GLC.ONE, GLC.MAX),
        DEST_OVER( GLC.SRC_ALPHA, GLC.ONE_MINUS_SRC_ALPHA, GLC.FUNC_ADD),
    }

    enum class ProgramType(
            internal val method: BlendMethod
    ) {
        SQARE_GRADIENT(MAX),
        CHANGE_COLOR(MAX),
        GRID(SRC_OVER),

        PASS_BASIC(SRC_OVER),
        PASS_BORDER(DEST_OVER),
        PASS_INVERT(MAX),
        PASS_RENDER(SRC_OVER),
        PASS_ESCALATE(SRC_OVER),

        STROKE_SPORE(MAX),
        STROKE_BASIC(SRC_OVER),
        STROKE_PIXEL(SRC_OVER),
        STROKE_V2_LINE_PASS(SRC_OVER),
        STROKE_AFTERPASS_INTENSIFY(SOURCE),

        POLY_RENDER(SRC_OVER),
        LINE_RENDER(SRC_OVER),
    }

    private fun initShaderPrograms(scriptService: IScriptService) : Array<IGLProgram> {
        return initShaders330(scriptService)
    }
    private fun initShaders330(scriptService: IScriptService) : Array<IGLProgram> {
        val array = Array<IGLProgram?>(ProgramType.values().size,{null})

        array[ProgramType.SQARE_GRADIENT.ordinal] = loadProgram(scriptService,
                "shaders/pass.vert",
                null,
                "shaders/square_grad.frag")
        array[ProgramType.STROKE_BASIC.ordinal] = loadProgram(scriptService,
                "shaders/brushes/stroke_basic.vert",
                "shaders/brushes/stroke_basic.geom",
                "shaders/brushes/stroke_basic.frag")
        array[ProgramType.CHANGE_COLOR.ordinal] = loadProgram(scriptService,
                "shaders/pass.vert",
                null,
                "shaders/pass_change_color.frag")
        array[ProgramType.PASS_BORDER.ordinal] = loadProgram(scriptService,
                "shaders/pass.vert",
                null,
                "shaders/pass_border.frag")
        array[ProgramType.PASS_INVERT.ordinal] = loadProgram(scriptService,
                "shaders/pass.vert",
                null,
                "shaders/pass_invert.frag")
        array[ProgramType.PASS_RENDER.ordinal] = loadProgram(scriptService,
                "shaders/pass.vert",
                null,
                "shaders/pass_render.frag")
        array[ProgramType.PASS_ESCALATE.ordinal] = loadProgram(scriptService,
                "shaders/pass.vert",
                null,
                "shaders/pass_escalate.frag")
        array[ProgramType.STROKE_SPORE.ordinal] = loadProgram(scriptService,
                "shaders/brushes/brush_spore.vert",
                "shaders/brushes/brush_spore.geom",
                "shaders/brushes/brush_spore.frag")
        array[ProgramType.PASS_BASIC.ordinal] = loadProgram(scriptService,
                "shaders/pass.vert",
                null,
                "shaders/pass_basic.frag")
        array[ProgramType.GRID.ordinal] = loadProgram(scriptService,
                "shaders/pass.vert",
                null,
                "shaders/etc/pass_grid.frag")
        array[ProgramType.STROKE_V2_LINE_PASS.ordinal] = loadProgram(scriptService,
                "shaders/brushes/stroke_pixel.vert",
                null,
                "shaders/brushes/stroke_pixel.frag")
        array[ProgramType.STROKE_PIXEL.ordinal] = array[ProgramType.STROKE_V2_LINE_PASS.ordinal]
        array[ProgramType.POLY_RENDER.ordinal] = loadProgram(scriptService,
                "shaders/shapes/poly_render.vert",
                null,
                "shaders/shapes/shape_render.frag")
        array[ProgramType.LINE_RENDER.ordinal] = loadProgram(scriptService,
                "shaders/shapes/line_render.vert",
                "shaders/shapes/line_render.geom",
                "shaders/shapes/shape_render.frag")
        array[ProgramType.STROKE_AFTERPASS_INTENSIFY.ordinal] = loadProgram(scriptService,
                "shaders/pass.vert",
                null,
                "shaders/brushes/brush_intensify.frag")

        return Array(ProgramType.values().size, {
            when (array[it]){
                null -> array[ProgramType.PASS_BASIC.ordinal]!!
                else -> array[it]!!
            }
        })
    }
    private fun loadProgram(scriptService: IScriptService, vert: String?, geom: String?, frag: String?) : IGLProgram{
        val shaders = mutableListOf<IGLShader>()

        if( vert != null) {
            shaders.add( compileShader( GLC.VERTEX_SHADER, scriptService.loadScript(vert)))
        }
        if( geom != null) {
            // This is kind of bad as it allows the user to declare a script that isn't actually there, but makes testing easier.
            val geomScript = scriptService.loadScript(geom)
            if( !geomScript.isBlank())
                shaders.add( compileShader( GLC.GEOMETRY_SHADER, geomScript))
        }
        if( frag != null) {
            shaders.add( compileShader( GLC.FRAGMENT_SHADER, scriptService.loadScript(frag)))
        }

        val program = linkProgram( shaders)

        shaders.forEach { it.delete() }

        return program
    }

    private fun compileShader(type: Int, source: String) : IGLShader {
        val shader = gl.createShader( type) ?: throw GLEException("Couldn't allocate OpenGL shader resources of type $type")
        gl.shaderSource(shader, source)
        gl.compileShader(shader)

        if( !gl.shaderCompiledSuccessfully(shader))
            throw GLEException("Failed to compile shader: ${gl.getShaderInfoLog(shader)}\n $source")

        return shader
    }

    private fun linkProgram( shaders: List<IGLShader>) : IGLProgram{
        val program = gl.createProgram() ?: throw GLEException("Couldn't allocate OpenGL program resources.")

        shaders.forEach { gl.attachShader(program, it) }
        gl.linkProgram( program)

        if( !gl.programLinkedSuccessfully( program))
            throw GLEException("Failed to link shader: ${gl.getProgramInfoLog(program)}\n")

        shaders.forEach { gl.detatchShader(program, it) }
        return program
    }

    // endregion
}


class GLEException(message: String) : Exception(message)