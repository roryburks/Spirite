package spirite.base.util.linear

import com.hackoeur.jglm.support.FastMath
import spirite.base.util.d
import spirite.base.util.f

/**
 * Created by Rory Burks on 4/28/2017.
 */

data class Vec2(
        val x : Float,
        val y : Float
) {
    val mag: Float by lazy {FastMath.sqrtFast(x * x + y * y)}

    operator fun minus( rhs: Vec2) = Vec2(x - rhs.x, y-rhs.y)
    operator fun plus( rhs: Vec2) = Vec2( x + rhs.x, y + rhs.y)
    operator fun times( rhs: Float) = Vec2(x*rhs, y*rhs)

    infix fun dot(rhs: Vec2) = this.x * rhs.x + this.y * rhs.y
    infix fun cross(rhs: Vec2) = x * rhs.y - y * rhs.x
    infix fun scalar(f: Float) = Vec2(x * f, y * f)

    fun normalize(): Vec2 {
        val isr = FastMath.invSqrtFast(x * x + y * y)
        return Vec2(this.x * isr, this.y * isr)
    }

    fun rotate(theta: Float):Vec2 {
        val cs = FastMath.cos(theta.d).f
        val sn = FastMath.sin(theta.d).f

        return Vec2(x*cs - y*sn, x*sn + y*cs)
    }

    override fun toString(): String {
        return "<$x,$y>"
    }

    companion object {
        val Zero = Vec2(0f,0f)
    }
}


data class Vec2i (
        val x: Int,
        val y: Int
){
    constructor(v: Vec2i): this(v.x, v.y) {}

    operator fun minus(rhs: Vec2i) = Vec2i(x - rhs.x, y - rhs.y)
    operator fun plus(rhs: Vec2i) = Vec2i(x + rhs.x, y + rhs.y)

    infix fun dot(rhs: Vec2i) = this.x * rhs.x + this.y * rhs.y

    companion object {
        val Zero = Vec2i(0,0)
    }
}
