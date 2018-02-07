package spirite.base.util.linear


data class Mat4(
        val m00 : Float, val m01 : Float, val m02 : Float, val m03 : Float,
        val m10 : Float, val m11 : Float, val m12 : Float, val m13 : Float,
        val m20 : Float, val m21 : Float, val m22 : Float, val m23 : Float,
        val m30 : Float, val m31 : Float, val m32 : Float, val m33 : Float
) {
    constructor() : this(
            0f,0f,0f,0f,
            0f,0f,0f,0f,
            0f,0f,0f,0f,
            0f,0f,0f,0f)
    constructor(diagonalValue : Float ) : this(
            diagonalValue,0f,0f,0f,
            0f,diagonalValue,0f,0f,
            0f,0f,diagonalValue,0f,
            0f,0f,0f,diagonalValue)

    // vec3 and vec4

    constructor( mat: FloatArray) : this(
            mat[0], mat[1], mat[2], mat[3],
            mat[4], mat[5], mat[6], mat[7],
            mat[8], mat[9], mat[10], mat[11],
            mat[12], mat[13], mat[14], mat[15])


    operator fun times(other: Mat4) : Mat4 {
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

        return Mat4(
                nm00, nm01, nm02, nm03,
                nm10, nm11, nm12, nm13,
                nm20, nm21, nm22, nm23,
                nm30, nm31, nm32, nm33)
    }


    fun transpose(): Mat4 {
        return Mat4(
                m00, m10, m20, m30,
                m01, m11, m21, m31,
                m02, m12, m22, m32,
                m03, m13, m23, m33
        )
    }
}