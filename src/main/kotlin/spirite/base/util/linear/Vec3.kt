package spirite.base.util.linear

import com.hackoeur.jglm.support.FastMath

data class Vec3(
        val x :Float,
        val y : Float,
        val z : Float
) {
    val mag: Float by lazy { FastMath.sqrtFast( x*x + y*y + z*z)}

    operator fun minus( rhs: Vec3) = Vec3( x - rhs.x, y - rhs.y, z - rhs.z)
    operator fun plus( rhs: Vec3) = Vec3( x + rhs.x, y + rhs.y, z + rhs.z)


    override fun toString() = "<$x,$y,$z>"

}
data class Vec3i(
        val x :Int,
        val y : Int,
        val z : Int
) {
    val mag: Float by lazy { FastMath.sqrtFast((x*x + y*y + z*z).toFloat())}

    operator fun minus( rhs: Vec3) = Vec3( x - rhs.x, y - rhs.y, z - rhs.z)
    operator fun plus( rhs: Vec3) = Vec3( x + rhs.x, y + rhs.y, z + rhs.z)


    override fun toString() = "<$x,$y,$z>"

}