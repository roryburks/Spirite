package spirite.base.util.linear

import com.hackoeur.jglm.support.FastMath

data class Vec4(
        val x :Float,
        val y : Float,
        val z : Float,
        val w : Float
){
    val mag: Float by lazy {FastMath.sqrtFast( x*x + y*y + z*z + w*w)}

    operator fun minus( rhs: Vec4) = Vec4( x - rhs.x, y - rhs.y, z - rhs.z, w - rhs.w)
    operator fun plus( rhs: Vec4) = Vec4( x + rhs.x, y + rhs.y, z + rhs.z, w + rhs.w)


    override fun toString() = "<$x,$y,$z,$w>"
}

data class Vec4i(
        val x : Int,
        val y : Int,
        val z : Int,
        val w : Int
) {
    val mag: Float by lazy {FastMath.sqrtFast((x*x + y*y + z*z + w*w).toFloat())}

    operator fun minus( rhs: Vec4i) = Vec4i( x - rhs.x, y - rhs.y, z - rhs.z, w - rhs.w)
    operator fun plus( rhs: Vec4i) = Vec4i( x + rhs.x, y + rhs.y, z + rhs.z, w + rhs.w)

    override fun toString() = "<$x,$y,$z,$w>"
}