package spirite.base.util.linear

import rb.vectrix.linear.ITransformF
import rb.vectrix.linear.Mat4f
import rb.glow.gl.IFloat32Source
import rb.glow.gl.IGL

// TODO: Figure out where this belongs (later).  Possibly in Glow
object MatrixBuilder {
    /** Creates a transform matrix representing an Orthagonal Projection
     * (A flat, rectangular projection in XY coordinates). */
    fun orthagonalProjectionMatrix(
            left: Float, right: Float,
            bottom: Float, top: Float,
            near: Float, far: Float
    ): FloatArray {
        return floatArrayOf(
                2f / (right - left), 0f, 0f, -(right + left) / (right - left),
                0f, 2f / (top - bottom), 0f, -(bottom + top) / (top - bottom),
                0f, 0f, -2f / (far - near), -(far + near) / (far - near),
                0f, 0f, 0f, 1f)
    }

    /** Converts a 3x3 AffineTransform into a Quaternion Transformation Matrix
     * which can be fed into OpenGL to behave in the expected way. */
    fun wrapTransformAs4x4(transform: ITransformF): FloatArray {
        return floatArrayOf(
                transform.m00f, transform.m01f, 0f, transform.m02f,
                transform.m10f, transform.m11f, 0f, transform.m12f,
                0f, 0f, 1f, 0f,
                0f, 0f, 0f, 1f)
    }

}

fun Mat4f.toIFloat32Source(gl: IGL): IFloat32Source {
    val source = gl.makeFloat32Source(16)
    source[0] = this.m00f; source[1] = this.m01f; source[2] = this.m02f; source[3] = this.m03f
    source[4] = this.m10f; source[5] = this.m11f; source[6] = this.m12f; source[7] = this.m13f
    source[8] = this.m20f; source[9] = this.m21f; source[10] = this.m22f; source[11] = this.m23f
    source[12] = this.m30f; source[13] = this.m31f; source[14] = this.m32f; source[15] = this.m33f
    return source
}