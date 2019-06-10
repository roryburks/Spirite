package rb.vectrix.linear

import rb.vectrix.mathUtil.d
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

abstract class Vec2
{
    abstract val x: Double
    abstract val y: Double

    operator fun minus(rhs: Vec2) : Vec2 =
        Vec2d(x - rhs.x, y - rhs.y)
    operator fun plus(rhs: Vec2) : Vec2 =
        Vec2d(x + rhs.x, y + rhs.y)
    operator fun times(rhs: Double) : Vec2 = Vec2d(x * rhs, y * rhs)

    infix fun dot(rhs: Vec2) : Double = this.x * rhs.x + this.y * rhs.y
    infix fun cross(rhs: Vec2) : Double= x * rhs.y - y  * rhs.x

    fun rotate(theta: Double) : Vec2 {
        val cs = cos(theta)
        val sn = sin(theta)

        return Vec2d(x * cs - y * sn, x * sn + y * cs)
    }

    abstract val normalized : Vec2

    companion object {
        val Zero get() = Vec2d.Zero
    }
}

data class Vec2d(
    override val x: Double,
    override val y: Double)
    : Vec2()
{
    val mag get() = sqrt(x * x + y * y)

    override val normalized : Vec2d get() {
        val isr = 1/ sqrt(x * x + y * y)
        return Vec2d(this.x * isr, this.y * isr)
    }

    override fun toString() = "<$x,$y>"

    companion object {
        val Zero = Vec2d(0.0, 0.0)
    }
}

data class Vec2f(
    val xf : Float,
    val yf : Float)
    : Vec2()
{
    override val x: Double get() = xf.d
    override val y: Double get() = yf.d

    val mag: Float get() = sqrt(xf * xf + yf * yf)

    operator fun minus( rhs: Vec2f) = Vec2f(xf - rhs.xf, yf - rhs.yf)
    operator fun plus( rhs: Vec2f) = Vec2f(xf + rhs.xf, yf + rhs.yf)
    operator fun times( rhs: Float) = Vec2f(xf * rhs, yf * rhs)

    infix fun dot(rhs: Vec2f) : Float = this.xf * rhs.xf + this.yf * rhs.yf
    infix fun cross(rhs: Vec2f) : Float = xf * rhs.yf - yf * rhs.xf

    fun rotate(theta: Float): Vec2f {
        val cs = cos(theta)
        val sn = sin(theta)

        return Vec2f(xf * cs - yf * sn, xf * sn + yf * cs)
    }

    override val normalized: Vec2f get() {
        val isr = 1/ sqrt(xf * xf + yf * yf)
        return Vec2f(this.xf * isr, this.yf * isr)
    }

    override fun toString() = "<$x,$y>"

    companion object {
        val Zero = Vec2f(0f, 0f)
    }
}


data class Vec2i (
    val xi: Int,
    val yi: Int)
    : Vec2()
{
    override val x: Double get() = xi.d
    override val y: Double get() = yi.d

    operator fun minus(rhs: Vec2i) = Vec2i(xi - rhs.xi, yi - rhs.yi)
    operator fun plus(rhs: Vec2i) = Vec2i(xi + rhs.xi, yi + rhs.yi)
    operator fun times( rhs: Int) = Vec2i(xi * rhs, yi * rhs)

    infix fun dot(rhs: Vec2i) : Int = this.xi * rhs.xi + this.yi * rhs.yi
    infix fun cross(rhs: Vec2i) : Int = xi * rhs.yi - yi * rhs.xi

    val mag get() = sqrt(x * x + y * y)
    override val normalized: Vec2 get() {
        val isr = 1/ sqrt(x * x + y * y)
        return Vec2d(this.x * isr, this.y * isr)
    }

    override fun toString() = "<$x,$y>"

    companion object {
        val Zero = Vec2i(0, 0)
    }
}
