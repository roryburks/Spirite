package rb.glow.gl.shader.programs

import rb.glow.gl.GLUniform
import rb.glow.gl.GLUniform1f
import rb.glow.gl.GLUniform3f
import rb.vectrix.linear.Vec3f

class PolyRenderCall(
        color: Vec3f,
        alpha: Float
) : IGlProgramCall {
    override val uniforms: List<GLUniform>? = listOf(
            GLUniform3f("u_color", color),
            GLUniform1f("u_alpha", alpha)
    )
    override val programKey: String get() = Key
    companion object{ val Key: String get() = "PolyRender"}
}
