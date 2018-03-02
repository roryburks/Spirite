package spirite.base.graphics.gl.shader

import spirite.base.graphics.gl.IGLProgram

interface IGLShaderLoader {
    fun initShaderPrograms() : Array<IGLProgram>
}