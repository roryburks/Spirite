package spirite.base.graphics.gl.shader

import spirite.base.graphics.gl.IGLProgram
import spirite.base.resources.IScriptService

interface IGLShaderLoader {
    fun initShaderPrograms() : Array<IGLProgram>
}