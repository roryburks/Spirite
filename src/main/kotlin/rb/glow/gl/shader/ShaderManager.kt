package rb.glow.gl.shader

import com.jogamp.opengl.GLException
import rb.glow.exceptions.GLEException
import rb.glow.gl.GLC
import rb.glow.gl.IGL
import rb.glow.gl.IGLProgram
import rb.glow.gl.IGLShader
import rb.glow.resources.IScriptService

interface IShaderManager {
    fun loadShaders( gl: IGL, map: Map<String, GlShaderLoadContract>)
    fun getShader( key: String) : IGLProgram
}

private const val GLOBAL = "#GLOBAL"

class ShaderManager(
    val scriptService: IScriptService,
    val globalFragScript: String
) : IShaderManager
{
    private val globalFrag : String by lazy { scriptService.loadScript(globalFragScript) }
    private val _shaderMap = mutableMapOf<String,IGLProgram>()

    override fun loadShaders(gl: IGL, map: Map<String, GlShaderLoadContract>) {
        map.forEach { key, k ->
            _shaderMap[key] = loadProgram(k, gl)
        }
    }

    override fun getShader(key: String) = _shaderMap[key] ?: throw GLException("Could not find shader by key: $key")


    private fun loadProgram( k: GlShaderLoadContract, gl: IGL) : IGLProgram {
        val shaders = mutableListOf<IGLShader>()

        if( k.vertShader != null) {
            shaders.add( compileShader( GLC.VERTEX_SHADER, scriptService.loadScript(k.vertShader), gl))
        }
        if( k.geomShader != null) {
            // This is kind of bad as it allows the user to declare a script that isn'loadEmUp actually there, but makes testing easier.
            val geomScript = scriptService.loadScript(k.geomShader)
            if( !geomScript.isBlank())
                shaders.add( compileShader( GLC.GEOMETRY_SHADER, geomScript, gl))
        }
        if( k.fragShader != null) {
            shaders.add( compileShader( GLC.FRAGMENT_SHADER, scriptService.loadScript(k.fragShader), gl))
        }

        val program = linkProgram( shaders, gl)

        shaders.forEach { it.delete() }

        return program
    }

    private fun compileShader(type: Int, source: String, gl: IGL) : IGLShader {
        val shader = gl.createShader( type) ?: throw GLEException("Couldn'loadEmUp allocate OpenGL shader resources of type $type")

        val linkedSource = source.replace(GLOBAL, globalFrag)
        gl.shaderSource(shader, linkedSource)
        gl.compileShader(shader)

        if( !gl.shaderCompiledSuccessfully(shader))
            throw GLEException("Failed to compile shader: ${gl.getShaderInfoLog(shader)}\n $source")

        return shader
    }

    private fun linkProgram( shaders: List<IGLShader>, gl: IGL) : IGLProgram {
        val program = gl.createProgram() ?: throw GLEException("Couldn'loadEmUp allocate OpenGL program resources.")

        shaders.forEach { gl.attachShader(program, it) }
        gl.linkProgram( program)

        if( !gl.programLinkedSuccessfully( program))
            throw GLEException("Failed to link shader: ${gl.getProgramInfoLog(program)}\n")

        shaders.forEach { gl.detatchShader(program, it) }
        return program
    }

}