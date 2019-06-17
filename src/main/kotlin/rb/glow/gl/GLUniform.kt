package rb.glow.gl

import rb.glow.glu.MatrixBuilder
import rb.vectrix.linear.Mat4f
import rb.vectrix.linear.Vec2f
import rb.vectrix.linear.Vec3f
import rb.vectrix.linear.Vec4f

sealed class GLUniform(
        val name: String
) {
    abstract fun apply(gl : IGL, program: IGLProgram)
    fun getUniformLocation(gl: IGL, program: IGLProgram) = gl.getUniformLocation(program, name)
}

class GLUniform1f(name: String, val x: Float) : GLUniform(name) {
    override fun apply(gl: IGL, program: IGLProgram)
    {gl.uniform1f(getUniformLocation(gl, program) ?: return, x)}
}
class GLUniform2f(name: String, val v: Vec2f) : GLUniform(name) {
    override fun apply(gl: IGL, program: IGLProgram)
    {gl.uniform2f(getUniformLocation(gl, program) ?: return, v.xf, v.yf)}
}
class GLUniform3f(name: String, val v: Vec3f) : GLUniform(name) {
    override fun apply(gl: IGL, program: IGLProgram)
    {gl.uniform3f(getUniformLocation(gl, program) ?: return, v.xf, v.yf, v.zf)}
}
class GLUniform4f(name: String, val v: Vec4f) : GLUniform(name) {
    override fun apply(gl: IGL, program: IGLProgram)
    {gl.uniform4f(getUniformLocation(gl, program) ?: return, v.xf, v.yf, v.zf, v.wf)}
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
class GLUniformMatrix4fv(name: String, val mat4: Mat4f) : GLUniform(name) {
    override fun apply(gl: IGL, program: IGLProgram)
    {gl.uniformMatrix4fv(getUniformLocation(gl,program) ?: return, MatrixBuilder.F.convertMat4ToFloat32(gl, mat4))}
}

class GLUniform1iv(name: String, val v: IntArray) : GLUniform(name) {
    override fun apply(gl: IGL, program: IGLProgram) {gl.uniform1iv(getUniformLocation(gl, program) ?: return, gl.makeInt32Source(v))}
}
