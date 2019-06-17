package rb.glow.gle

import rb.glow.gl.IGLProgram

interface IGLShaderLoader {
    fun initShaderPrograms() : Map<String,IGLProgram>
}