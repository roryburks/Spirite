package spirite.base.graphics.gl.shader

import spirite.base.graphics.gl.*
import spirite.base.resources.IScriptService
import spirite.base.util.glu.GLC

class GL330ShaderLoader( val gl: IGL, val scriptService: IScriptService) : IGLShaderLoader {
    val globalFrag : String by lazy { scriptService.loadScript("shaders/global.frag") }
    val GLOBAL = "#GLOBAL"

    override fun initShaderPrograms(): Array<IGLProgram> {

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

        val linkedSource = source.replace(GLOBAL, globalFrag)
        gl.shaderSource(shader, linkedSource)
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

}