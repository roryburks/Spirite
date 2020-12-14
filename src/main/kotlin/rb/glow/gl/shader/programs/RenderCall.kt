package rb.glow.gl.shader.programs

import rb.glow.gl.GLUniform
import rb.glow.gl.GLUniform1f
import rb.glow.gl.GLUniform1iv

class RenderCall(
        alpha: Float,
        calls: List<Pair<RenderAlgorithm, Int>>
) : IGlProgramCall
{
    private val MAX_CALLS = 10

    enum class RenderAlgorithm( val progId: Int) {
        //STRAIGHT_PASS(0), // Adding this would be redundant
        AS_COLOR(1),
        AS_COLOR_ALL(2),
        DISSOLVE(3)
    }

    override val uniforms: List<GLUniform>? = listOf(
            GLUniform1f("u_alpha", alpha),
            GLUniform1iv(
                    "u_values",
                    IntArray(MAX_CALLS) { calls.getOrNull(it)?.second ?: 0 }
            ),
            GLUniform1iv(
                    "u_composites",
                    IntArray(MAX_CALLS) { calls.getOrNull(it)?.first?.progId ?: 0 }
            )
    )
    override val programKey: String get() = Key
    companion object { val Key: String = "RenderProg"}
}
