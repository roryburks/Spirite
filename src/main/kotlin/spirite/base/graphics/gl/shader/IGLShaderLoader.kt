package spirite.base.graphics.gl.shader

import rb.glow.gl.IGLProgram

interface IGLShaderLoader {
    fun initShaderPrograms() : Map<String,IGLProgram>
}