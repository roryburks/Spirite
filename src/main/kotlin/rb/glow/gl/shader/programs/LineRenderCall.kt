package rb.glow.gl.shader.programs

import rb.glow.JoinMethod
import rb.glow.gl.GLUniform
import rb.glow.gl.GLUniform1f
import rb.glow.gl.GLUniform1i
import rb.glow.gl.GLUniform3f
import rb.vectrix.linear.Vec3f

class LineRenderCall(
        joinMethod: JoinMethod,
        lineWidth: Float,
        color: Vec3f,
        alpha: Float)
    : IGlProgramCall
{
    override val uniforms: List<GLUniform>? = listOf(
            GLUniform1i("u_join", when (joinMethod) {
                JoinMethod.BEVEL -> 1 // 2
                JoinMethod.MITER -> 1
                JoinMethod.ROUNDED -> 1 // 0
            }),
            GLUniform1f("u_width", lineWidth / 2f),
            GLUniform3f("u_color", color),
            GLUniform1f("u_alpha", alpha)
    )

    override val programKey: String get() = Key
    companion object { const val Key = "LINE_RENDER"}
}