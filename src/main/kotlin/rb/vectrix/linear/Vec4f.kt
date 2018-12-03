package rb.vectrix.linear

import rb.vectrix.mathUtil.d


abstract class Vec4{
    abstract val x: Double
    abstract val y: Double
    abstract val z: Double
    abstract val w: Double

    // TODO:
    //operator fun plus( rhs: Vec3) : Vec3
    //operator fun minus( rhs: Vec3) : Vec3
    //operator fun times( rhs: Double)
    // etc
}

data class Vec4d(
        override val x: Double,
        override val y: Double,
        override val z: Double,
        override val w: Double)
    : Vec4()

data class Vec4f(
        val xf: Float,
        val yf: Float,
        val zf: Float,
        val wf: Float)
    : Vec4()
{
    override val x: Double get() = xf.d
    override val y: Double get() = yf.d
    override val z: Double get() = zf.d
    override val w: Double get() = wf.d
}