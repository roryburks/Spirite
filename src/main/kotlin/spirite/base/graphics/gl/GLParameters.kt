package spirite.base.graphics.gl

import spirite.base.util.linear.*

data class GLParameters(
        var width : Int,
        var heigth: Int,
        var flip: Boolean = false,
        var clipRect : Rect? = null,
        var premultiplied: Boolean = false,

        var texture1 : GLImage? = null,
        var texture2 : GLImage? = null,

        var useBlendMode: Boolean = true,
        var useDefaultBlendMode: Boolean = true,
        var bm_sfc: Int = 0,
        var bm_sfa: Int = 0,
        var bm_dfc: Int = 0,
        var bm_dfa: Int = 0,
        var bm_fc: Int = 0,
        var bm_fa: Int = 0
) {
    fun setBlendMode(src_factor: Int, dst_factor: Int, formula: Int) {
        useDefaultBlendMode = false
        bm_sfa = src_factor
        bm_sfc = bm_sfa
        bm_dfa = dst_factor
        bm_dfc = bm_dfa
        bm_fa = formula
        bm_fc = bm_fa
    }

    fun setBlendModeExt(
            src_factor_color: Int, dst_factor_color: Int, formula_color: Int,
            src_factor_alpha: Int, dst_factor_alpha: Int, formula_alpha: Int) {
        useDefaultBlendMode = false
        bm_sfc = src_factor_color
        bm_dfc = dst_factor_color
        bm_fc = formula_color

        bm_sfa = src_factor_alpha
        bm_dfa = dst_factor_alpha
        bm_fa = formula_alpha

    }
}

sealed class GLUniform(
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
