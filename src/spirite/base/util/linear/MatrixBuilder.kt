package spirite.base.util.linear

import com.hackoeur.jglm.Mat4
import spirite.base.graphics.gl.IFloat32Source
import spirite.base.graphics.gl.IGL

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
                0f, 2f / (bottom - top), 0f, -(bottom + top) / (bottom - top),
                0f, 0f, -2f / (far - near), -(far + near) / (far - near),
                0f, 0f, 0f, 1f
        )
    }

    /** Converts a 3x3 AffineTransform into a Quaternion Transformation Matrix
     * which can be fed into OpenGL to behave in the expected way. */
    fun wrapTransformAs4x4(transform: Transform): FloatArray {
        return floatArrayOf(
                transform.m00, transform.m01, 0f, transform.m02,
                transform.m10, transform.m11, 0f, transform.m12,
                0f, 0f, 1f, 0f,
                0f, 0f, 0f, 1f)
    }

}

fun Mat4.toIFloat32Source(gl: IGL): IFloat32Source {
    val source = gl.makeFloat32Source(16)
    source[0] = this.m00; source[1] = this.m01; source[2] = this.m02; source[3] = this.m03
    source[4] = this.m10; source[5] = this.m11; source[6] = this.m12; source[7] = this.m13
    source[8] = this.m20; source[9] = this.m21; source[10] = this.m22; source[11] = this.m23
    source[12] = this.m30; source[13] = this.m31; source[14] = this.m32; source[15] = this.m33
    return source
}