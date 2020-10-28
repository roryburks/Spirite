package rb.glow.gl.shader.programs

import rb.glow.gl.GLUniform

class BasicCall() : IGlProgramCall {
    override val uniforms: List<GLUniform>? = null

    override val programKey: String get() = Key
    companion object { const val Key = "PASS_BASIC"}
}