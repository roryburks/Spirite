package rb.glow.gl.shader.programs

import rb.glow.gl.GLUniform
import rb.glow.gle.IGLEngine

interface IGlProgramCall {
    val uniforms: List<GLUniform>?
    val programKey: String
    val method: IGLEngine.BlendMethod get() = IGLEngine.BlendMethod.SRC_OVER
    val lineSmoothing : Boolean get() = false
}