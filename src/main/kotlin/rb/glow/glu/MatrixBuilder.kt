package rb.glow.glu

import rb.glow.gl.IFloat32Source
import rb.glow.gl.IGL
import rb.vectrix.linear.ITransform
import rb.vectrix.linear.ITransformF
import rb.vectrix.linear.Mat4f
import rb.vectrix.mathUtil.f


object MatrixBuilder {
    object F {

        fun orthagonalProjectionMatrix(
                left: Float, right: Float,
                bottom: Float, top: Float,
                near: Float, far: Float) = Mat4f(
                2f / (right - left), 0f, 0f, -(right + left) / (right - left),
                0f, 2f / (top - bottom), 0f, -(bottom + top) / (top - bottom),
                0f, 0f, -2f / (far - near), -(far + near) / (far - near),
                0f, 0f, 0f, 1f)

        fun wrapTransform( transform: ITransform) = Mat4f(
                transform.m00.f, transform.m01.f, 0f, transform.m02.f,
                transform.m10.f, transform.m11.f, 0f, transform.m12.f,
                0f, 0f, 1f, 0f,
                0f, 0f, 0f, 1f)
        fun wrapTransformF( transform: ITransformF) = Mat4f(
                transform.m00f, transform.m01f, 0f, transform.m02f,
                transform.m10f, transform.m11f, 0f, transform.m12f,
                0f, 0f, 1f, 0f,
                0f, 0f, 0f, 1f)

        fun convertMat4ToFloat32(gl: IGL, mat: Mat4f) : IFloat32Source {
            val source = gl.makeFloat32Source(16)
            source[0] = mat.m00f; source[1] = mat.m01f; source[2] = mat.m02f; source[3] = mat.m03f
            source[4] = mat.m10f; source[5] = mat.m11f; source[6] = mat.m12f; source[7] = mat.m13f
            source[8] = mat.m20f; source[9] = mat.m21f; source[10] = mat.m22f; source[11] = mat.m23f
            source[12] = mat.m30f; source[13] = mat.m31f; source[14] = mat.m32f; source[15] = mat.m33f
            return source
        }
    }
}