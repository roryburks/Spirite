package spirite.base.graphics.gl

import com.hackoeur.jglm.Mat4
import spirite.base.util.linear.Rect
import spirite.base.util.linear.toIFloat32Source

interface GLParameters {
    val width : Int
    val heigth: Int
    val flip: Boolean
    val clipRect : Rect?
    val uniforms: List<GLUniform>?

    val texture1 : GLImage?
    val texture2 : GLImage?

    val useBlendMode: Boolean
    val useDefaultBlendMode: Boolean
    val bm_sfc: Int
    val bm_sfa: Int
    val bm_dfc: Int
    val bm_dfa: Int
    val bm_fc: Int
    val bm_fa: Int

}

data class GLParametersMutable (
        override var width : Int,
        override var heigth: Int,
        override var flip: Boolean = false,
        override var clipRect : Rect? = null,
        override var uniforms: List<GLUniform>? = null,

        override var texture1 : GLImage? = null,
        override var texture2 : GLImage? = null,

        override var useBlendMode: Boolean = true,
        override var useDefaultBlendMode: Boolean = true,
        override var bm_sfc: Int = 0,
        override var bm_sfa: Int = 0,
        override var bm_dfc: Int = 0,
        override var bm_dfa: Int = 0,
        override var bm_fc: Int = 0,
        override var bm_fa: Int = 0
) : GLParameters

sealed abstract class GLUniform(
        val name: String
) {
    abstract fun apply( gl : IGL, program: IGLProgram)
    fun getUniformLocation( gl: IGL, program: IGLProgram) = gl.getUniformLocation(program, name)
}

class GLUniform1f(name: String, val x: Float) : GLUniform(name) {
    override fun apply(gl: IGL, program: IGLProgram)
    {gl.uniform1f(getUniformLocation(gl, program) ?: return, x)}
}
class GLUniform2f(name: String, val x: Float, val y: Float) : GLUniform(name) {
    override fun apply(gl: IGL, program: IGLProgram)
    {gl.uniform2f(getUniformLocation(gl, program) ?: return, x, y)}
}
class GLUniform3f(name: String, val x: Float, val y: Float, val z: Float) : GLUniform(name) {
    override fun apply(gl: IGL, program: IGLProgram)
    {gl.uniform3f(getUniformLocation(gl, program) ?: return, x, y, z)}
}
class GLUniform4f(name: String, val x: Float, val y: Float, val z: Float, val w: Float) : GLUniform(name) {
    override fun apply(gl: IGL, program: IGLProgram)
    {gl.uniform4f(getUniformLocation(gl, program) ?: return, x, y, z, w)}
}
class GLUniformMatrix4fv(name: String, val mat4: Mat4) : GLUniform(name) {
    override fun apply(gl: IGL, program: IGLProgram)
    {gl.uniformMatrix4fv(getUniformLocation(gl,program) ?: return, mat4.toIFloat32Source(gl))}
}
