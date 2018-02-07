package spirite.base.graphics.gl

import spirite.base.util.linear.*

interface GLParameters {
    val width : Int
    val heigth: Int
    val flip: Boolean
    val clipRect : Rect?

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
) : GLParameters {
    fun setBlendMode(src_factor: Int, dst_factor: Int, formula: Int) {
        useDefaultBlendMode = false
        bm_sfa = src_factor
        bm_sfc = bm_sfa
        bm_dfa = dst_factor
        bm_dfc = bm_dfa
        bm_fa = formula
        bm_fc = bm_fa
    }
}

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
class GLUniform2f(name: String, val v: Vec2) : GLUniform(name) {
    override fun apply(gl: IGL, program: IGLProgram)
    {gl.uniform2f(getUniformLocation(gl, program) ?: return, v.x, v.y)}
}
class GLUniform3f(name: String, val v: Vec3) : GLUniform(name) {
    override fun apply(gl: IGL, program: IGLProgram)
    {gl.uniform3f(getUniformLocation(gl, program) ?: return, v.x, v.y, v.z)}
}
class GLUniform4f(name: String, val v: Vec4) : GLUniform(name) {
    override fun apply(gl: IGL, program: IGLProgram)
    {gl.uniform4f(getUniformLocation(gl, program) ?: return, v.x, v.y, v.z, v.w)}
}

class GLUniform1i(name: String, val x: Int) : GLUniform(name) {
    override fun apply(gl: IGL, program: IGLProgram)
    {gl.uniform1i(getUniformLocation(gl, program) ?: return, x)}
}
class GLUniform2i(name: String, val x: Int, val y: Int) : GLUniform(name) {
    override fun apply(gl: IGL, program: IGLProgram)
    {gl.uniform2i(getUniformLocation(gl, program) ?: return, x, y)}
}
class GLUniform3i(name: String, val x: Int, val y: Int, val z: Int) : GLUniform(name) {
    override fun apply(gl: IGL, program: IGLProgram)
    {gl.uniform3i(getUniformLocation(gl, program) ?: return, x, y, z)}
}
class GLUniform4i(name: String, val x: Int, val y: Int, val z: Int, val w: Int) : GLUniform(name) {
    override fun apply(gl: IGL, program: IGLProgram)
    {gl.uniform4i(getUniformLocation(gl, program) ?: return, x, y, z, w)}
}
class GLUniformMatrix4fv(name: String, val mat4: Mat4) : GLUniform(name) {
    override fun apply(gl: IGL, program: IGLProgram)
    {gl.uniformMatrix4fv(getUniformLocation(gl,program) ?: return, mat4.toIFloat32Source(gl))}
}
