package rb.vectrix.linear

import rb.vectrix.mathUtil.d

interface IMat4 {
    val m00 : Double
    val m01 : Double
    val m02 : Double
    val m03 : Double

    val m10 : Double
    val m11 : Double
    val m12 : Double
    val m13 : Double

    val m20 : Double
    val m21 : Double
    val m22 : Double
    val m23 : Double

    val m30 : Double
    val m31 : Double
    val m32 : Double
    val m33 : Double

    operator fun times( other: IMat4) :Mat4d {
        val nm00 = this.m00 * other.m00 + this.m10 * other.m01 + this.m20 * other.m02 + this.m30 * other.m03
        val nm01 = this.m01 * other.m00 + this.m11 * other.m01 + this.m21 * other.m02 + this.m31 * other.m03
        val nm02 = this.m02 * other.m00 + this.m12 * other.m01 + this.m22 * other.m02 + this.m32 * other.m03
        val nm03 = this.m03 * other.m00 + this.m13 * other.m01 + this.m23 * other.m02 + this.m33 * other.m03
        val nm10 = this.m00 * other.m10 + this.m10 * other.m11 + this.m20 * other.m12 + this.m30 * other.m13
        val nm11 = this.m01 * other.m10 + this.m11 * other.m11 + this.m21 * other.m12 + this.m31 * other.m13
        val nm12 = this.m02 * other.m10 + this.m12 * other.m11 + this.m22 * other.m12 + this.m32 * other.m13
        val nm13 = this.m03 * other.m10 + this.m13 * other.m11 + this.m23 * other.m12 + this.m33 * other.m13
        val nm20 = this.m00 * other.m20 + this.m10 * other.m21 + this.m20 * other.m22 + this.m30 * other.m23
        val nm21 = this.m01 * other.m20 + this.m11 * other.m21 + this.m21 * other.m22 + this.m31 * other.m23
        val nm22 = this.m02 * other.m20 + this.m12 * other.m21 + this.m22 * other.m22 + this.m32 * other.m23
        val nm23 = this.m03 * other.m20 + this.m13 * other.m21 + this.m23 * other.m22 + this.m33 * other.m23
        val nm30 = this.m00 * other.m30 + this.m10 * other.m31 + this.m20 * other.m32 + this.m30 * other.m33
        val nm31 = this.m01 * other.m30 + this.m11 * other.m31 + this.m21 * other.m32 + this.m31 * other.m33
        val nm32 = this.m02 * other.m30 + this.m12 * other.m31 + this.m22 * other.m32 + this.m32 * other.m33
        val nm33 = this.m03 * other.m30 + this.m13 * other.m31 + this.m23 * other.m32 + this.m33 * other.m33

        return Mat4d(
            nm00, nm01, nm02, nm03,
            nm10, nm11, nm12, nm13,
            nm20, nm21, nm22, nm23,
            nm30, nm31, nm32, nm33
        )
    }

    operator fun times( other: Double) : IMat4 {
        return Mat4d(
            m00 * other, m01 * other, m02 * other, m03 * other,
            m10 * other, m11 * other, m12 * other, m13 * other,
            m20 * other, m21 * other, m22 * other, m23 * other,
            m30 * other, m31 * other, m32 * other, m33 * other
        )
    }

    val transpose : IMat4
}

data class Mat4d(
    override val m00 : Double, override val m01 : Double, override val m02 : Double, override val m03 : Double,
    override val m10 : Double, override val m11 : Double, override val m12 : Double, override val m13 : Double,
    override val m20 : Double, override val m21 : Double, override val m22 : Double, override val m23 : Double,
    override val m30 : Double, override val m31 : Double, override val m32 : Double, override val m33 : Double)
    : IMat4
{
    override val transpose: Mat4d
        get() = Mat4d(
            m00, m10, m20, m30,
            m01, m11, m21, m31,
            m02, m12, m22, m32,
            m03, m13, m23, m33
        )

    companion object {
        val Identity = Mat4d(
            1.0, 0.0, 0.0, 0.0,
            0.0, 1.0, 0.0, 0.0,
            0.0, 0.0, 1.0, 0.0,
            0.0, 0.0, 0.0, 1.0
        )
    }
}

data class Mat4f(
    val m00f : Float, val m01f : Float, val m02f : Float, val m03f : Float,
    val m10f : Float, val m11f : Float, val m12f : Float, val m13f : Float,
    val m20f : Float, val m21f : Float, val m22f : Float, val m23f : Float,
    val m30f : Float, val m31f : Float, val m32f : Float, val m33f : Float)
    : IMat4
{
    constructor(floatArray: FloatArray) : this(
            floatArray[0], floatArray[1], floatArray[2], floatArray[3],
            floatArray[4], floatArray[5], floatArray[6], floatArray[7],
            floatArray[8], floatArray[9], floatArray[10], floatArray[11],
            floatArray[12], floatArray[13], floatArray[14], floatArray[15])

    override val m00 get() = m00f.d
    override val m01 get() = m01f.d
    override val m02 get() = m02f.d
    override val m03 get() = m03f.d
    override val m10 get() = m10f.d
    override val m11 get() = m11f.d
    override val m12 get() = m12f.d
    override val m13 get() = m13f.d
    override val m20 get() = m20f.d
    override val m21 get() = m21f.d
    override val m22 get() = m22f.d
    override val m23 get() = m23f.d
    override val m30 get() = m30f.d
    override val m31 get() = m31f.d
    override val m32 get() = m32f.d
    override val m33 get() = m33f.d

    operator fun times( other: Mat4f) :Mat4f {
        val nm00 = this.m00f * other.m00f + this.m10f * other.m01f + this.m20f * other.m02f + this.m30f * other.m03f
        val nm01 = this.m01f * other.m00f + this.m11f * other.m01f + this.m21f * other.m02f + this.m31f * other.m03f
        val nm02 = this.m02f * other.m00f + this.m12f * other.m01f + this.m22f * other.m02f + this.m32f * other.m03f
        val nm03 = this.m03f * other.m00f + this.m13f * other.m01f + this.m23f * other.m02f + this.m33f * other.m03f
        val nm10 = this.m00f * other.m10f + this.m10f * other.m11f + this.m20f * other.m12f + this.m30f * other.m13f
        val nm11 = this.m01f * other.m10f + this.m11f * other.m11f + this.m21f * other.m12f + this.m31f * other.m13f
        val nm12 = this.m02f * other.m10f + this.m12f * other.m11f + this.m22f * other.m12f + this.m32f * other.m13f
        val nm13 = this.m03f * other.m10f + this.m13f * other.m11f + this.m23f * other.m12f + this.m33f * other.m13f
        val nm20 = this.m00f * other.m20f + this.m10f * other.m21f + this.m20f * other.m22f + this.m30f * other.m23f
        val nm21 = this.m01f * other.m20f + this.m11f * other.m21f + this.m21f * other.m22f + this.m31f * other.m23f
        val nm22 = this.m02f * other.m20f + this.m12f * other.m21f + this.m22f * other.m22f + this.m32f * other.m23f
        val nm23 = this.m03f * other.m20f + this.m13f * other.m21f + this.m23f * other.m22f + this.m33f * other.m23f
        val nm30 = this.m00f * other.m30f + this.m10f * other.m31f + this.m20f * other.m32f + this.m30f * other.m33f
        val nm31 = this.m01f * other.m30f + this.m11f * other.m31f + this.m21f * other.m32f + this.m31f * other.m33f
        val nm32 = this.m02f * other.m30f + this.m12f * other.m31f + this.m22f * other.m32f + this.m32f * other.m33f
        val nm33 = this.m03f * other.m30f + this.m13f * other.m31f + this.m23f * other.m32f + this.m33f * other.m33f

        return Mat4f(
            nm00, nm01, nm02, nm03,
            nm10, nm11, nm12, nm13,
            nm20, nm21, nm22, nm23,
            nm30, nm31, nm32, nm33
        )
    }

    override val transpose: Mat4f
        get() = Mat4f(
            m00f, m10f, m20f, m30f,
            m01f, m11f, m21f, m31f,
            m02f, m12f, m22f, m32f,
            m03f, m13f, m23f, m33f
        )

    companion object {
        val Identity = Mat4f(
            1f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f,
            0f, 0f, 1f, 0f,
            0f, 0f, 0f, 1f
        )
    }
}