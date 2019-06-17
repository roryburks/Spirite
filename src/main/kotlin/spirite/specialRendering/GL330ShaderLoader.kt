package spirite.specialRendering

import rb.glow.exceptions.GLEException
import rb.glow.gl.GLC
import rb.glow.gl.IGL
import rb.glow.gl.IGLProgram
import rb.glow.gl.IGLShader
import rb.glow.gle.*
import spirite.base.resources.IScriptService


class GL330ShaderLoader(val gl: IGL, val scriptService: IScriptService) : IGLShaderLoader {
    private val root = "shaders/330"
    private val GLOBAL = "#GLOBAL"

    val globalFrag : String by lazy { scriptService.loadScript("${root}/global.frag") }

    override fun initShaderPrograms(): Map<String,IGLProgram> {

        val array = mutableMapOf<String,IGLProgram>()

        // Brushes
        val default = loadProgram(
                "${root}/brushes/stroke_basic.vert",
                "${root}/brushes/stroke_basic.geom",
                "${root}/brushes/stroke_basic.frag")
        array[BasicCall.Key] = default
//        array[ProgramType.STROKE_SPORE.ordinal] = loadProgram(
//                "${root}/brushes/brush_spore.vert",
//                "${root}/brushes/brush_spore.geom",
//                "${root}/brushes/brush_spore.frag")
        array[StrokeV2LinePass.Key] = loadProgram(
                "${root}/brushes/stroke_pixel.vert",
                null,
                "${root}/brushes/stroke_pixel.frag")
        array[StrokePixelCall.Key] = array[StrokeV2LinePass.Key]!!
        array[StrokeV2ApplyCall.Key] = loadProgram(
                "${root}/pass.vert",
                null,
                "${root}/brushes/stroke_intensify.frag")
        array[StrokeApplyCall.Key] = loadProgram(
                "${root}/pass.vert",
                null,
                "${root}/brushes/stroke_apply.frag")
        array[StrokeV3LinePass.Key] = loadProgram(
                "${root}/brushes/stroke_v3_line.vert",
                null,
                "${root}/brushes/stroke_v3_line.frag")

        // Constructions
        array[SquareGradientCall.Key] = loadProgram(
                "${root}/pass.vert",
                null,
                "${root}/constructions/square_grad.frag")
        array[GridCall.Key] = loadProgram(
                "${root}/pass.vert",
                null,
                "${root}/constructions/pass_grid.frag")

        // Shapes
        array[PolyRenderCall.Key] = loadProgram(
                "${root}/shapes/poly_render.vert",
                null,
                "${root}/shapes/shape_render.frag")
        array[LineRenderCall.Key] = loadProgram(
                "${root}/shapes/line_render.vert",
                "${root}/shapes/line_render.geom",
                "${root}/shapes/shape_render.frag")

        // Filters
        array[ChangeColorCall.Key] = loadProgram(
                "${root}/pass.vert",
                null,
                "${root}/filters/pass_change_color.frag")
        array[InvertCall.Key] = loadProgram(
                "${root}/pass.vert",
                null,
                "${root}/filters/pass_invert.frag")

        // Special
        array[FillAfterpassCall.Key] = loadProgram(
                "${root}/pass.vert",
                null,
                "${root}/special/pass_fill.frag")
        array[BorderCall.Key] = loadProgram(
                "${root}/pass.vert",
                null,
                "${root}/special/pass_border.frag")

        // Render
        array[RenderCall.Key] = loadProgram(
                "${root}/pass.vert",
                null,
                "${root}/render/pass_render.frag")
        array[BasicCall.Key] = loadProgram(
                "${root}/pass.vert",
                null,
                "${root}/render/pass_basic.frag")


        return array
    }

    fun loadProgram(vert: String?, geom: String?, frag: String?) : IGLProgram {
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

        val linkedSource = source.replace(GLOBAL, globalFrag)
        gl.shaderSource(shader, linkedSource)
        gl.compileShader(shader)

        if( !gl.shaderCompiledSuccessfully(shader))
            throw GLEException("Failed to compile shader: ${gl.getShaderInfoLog(shader)}\n $source")

        return shader
    }

    private fun linkProgram( shaders: List<IGLShader>) : IGLProgram {
        val program = gl.createProgram() ?: throw GLEException("Couldn't allocate OpenGL program resources.")

        shaders.forEach { gl.attachShader(program, it) }
        gl.linkProgram( program)

        if( !gl.programLinkedSuccessfully( program))
            throw GLEException("Failed to groupLink shader: ${gl.getProgramInfoLog(program)}\n")

        shaders.forEach { gl.detatchShader(program, it) }
        return program
    }

}