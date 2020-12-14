package rb.glow.gl.shader.programs

import rb.glow.gl.GLUniform
import rb.glow.gl.GLUniform1f

class SimpleRenderCall(alpha: Float)
    : IGlProgramCall
{
    override val uniforms: List<GLUniform>? = listOf(
        GLUniform1f("u_alpha", alpha)
    )
    override val programKey: String get() = Key
    companion object { val Key = "SimpleRender"}
}
