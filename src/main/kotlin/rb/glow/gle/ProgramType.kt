package rb.glow.gle

import rb.glow.JoinMethod
import rb.glow.JoinMethod.*
import rb.glow.gl.*
import rb.glow.gle.IGLEngine.BlendMethod
import rb.glow.gle.IGLEngine.BlendMethod.SRC_OVER
import rb.vectrix.linear.Vec3f

interface IGlProgramCall {
    val uniforms: List<GLUniform>?
    val programKey: String
    val method: BlendMethod get() = SRC_OVER
    val lineSmoothing : Boolean get() = false
}

class BasicCall() : IGlProgramCall {
    override val uniforms: List<GLUniform>? = null

    override val programKey: String get() = Key
    companion object { const val Key = "PASS_BASIC"}
}

class RenderCall(
        alpha: Float,
        calls: List<Pair<RenderAlgorithm, Int>>)
    : IGlProgramCall
{
    val MAX_CALLS = 10

    enum class RenderAlgorithm( val progId: Int) {
        //STRAIGHT_PASS(0), // Adding this would be redundant
        AS_COLOR(1),
        AS_COLOR_ALL(2),
        DISSOLVE(3)
    }

    override val uniforms: List<GLUniform>? = listOf(
            GLUniform1f("u_alpha", alpha),
            GLUniform1iv("u_values", IntArray(MAX_CALLS, { calls.getOrNull(it)?.second ?: 0 })),
            GLUniform1iv("u_composites", IntArray(MAX_CALLS, { calls.getOrNull(it)?.first?.progId ?: 0 }))
    )

    override val programKey: String get() = Key
    companion object { const val Key = "PASS_RENDER"}
}

class PolyRenderCall(
        color: Vec3f,
        alpha: Float)
    : IGlProgramCall
{
    override val uniforms: List<GLUniform>? = listOf(
            GLUniform3f("u_color", color),
            GLUniform1f("u_alpha", alpha))

    override val programKey: String get() = Key
    companion object { const val Key = "POLY_RENDER"}
}

class LineRenderCall(
        joinMethod: JoinMethod,
        lineWidth: Float,
        color: Vec3f,
        alpha: Float)
    : IGlProgramCall
{
    override val uniforms: List<GLUniform>? = listOf(
            GLUniform1i("u_join", when (joinMethod) {
                BEVEL -> 1 // 2
                MITER -> 1
                ROUNDED -> 1 // 0
            }),
            GLUniform1f("u_width", lineWidth / 2f),
            GLUniform3f("u_color", color),
            GLUniform1f("u_alpha", alpha))

    override val programKey: String get() = Key
    companion object { const val Key = "LINE_RENDER"}
}