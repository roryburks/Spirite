package rb.vectrix.linear

import rb.vectrix.mathUtil.d


abstract class Vec3{
    abstract val x: Double
    abstract val y: Double
    abstract val z: Double

    operator fun plus( rhs: Vec3) : Vec3 = Vec3d(x + rhs.x, y + rhs.y, z + rhs.z)
    operator fun minus( rhs: Vec3) : Vec3 = Vec3d(x - rhs.x, y - rhs.y, z - rhs.z)
    operator fun times( rhs: Double) : Vec3 = Vec3d(x - rhs, y - rhs, z - rhs)

    override fun toString() = "<$x,$y,$z>"
}

data class Vec3d(
        override val x: Double,
        override val y: Double,
        override val z: Double)
    : Vec3()

data class Vec3f(
        val xf: Float,
        val yf: Float,
        val zf: Float)
    : Vec3()
{
    override val x: Double get() = xf.d
    override val y: Double get() = yf.d
    override val z: Double get() = zf.d

    operator fun plus( rhs: Vec3f) : Vec3f = Vec3f(xf + rhs.xf, yf + rhs.yf, zf + rhs.zf)
    operator fun minus( rhs: Vec3f) : Vec3f = Vec3f(xf - rhs.xf, yf - rhs.yf, zf - rhs.zf)
    operator fun times( rhs: Float) : Vec3f = Vec3f(xf - rhs, yf - rhs, zf - rhs)
}

data class Vec3i(
        val xi: Int,
        val yi: Int,
        val zi: Int)
    : Vec3()
{
    override val x: Double get() = xi.d
    override val y: Double get() = yi.d
    override val z: Double get() = zi.d

    operator fun plus( rhs: Vec3i) : Vec3i = Vec3i(xi + rhs.xi, yi + rhs.yi, zi + rhs.zi)
    operator fun minus( rhs: Vec3i) : Vec3i = Vec3i(xi - rhs.xi, yi - rhs.yi, zi - rhs.zi)
    operator fun times( rhs: Int) : Vec3i = Vec3i(xi - rhs, yi - rhs, zi - rhs)

}