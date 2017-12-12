package spirite.base.util.linear

import com.hackoeur.jglm.support.FastMath

/**
 * Created by Rory Burks on 4/28/2017.
 */

class Vec2(
        val x : Float,
        val y : Float
) {
    constructor(vec2: Vec2) : this( vec2.x, vec2.y){}

    val mag: Float by lazy {FastMath.sqrtFast(x * x + y * y)}

    operator fun minus( rhs: Vec2) = Vec2(x - rhs.x, y-rhs.y)
    operator fun plus( rhs: Vec2) = Vec2( x + rhs.x, y + rhs.y)

    infix fun dot(rhs: Vec2) = this.x * rhs.x + this.y * rhs.y
    infix fun cross(rhs: Vec2) = x * rhs.y - y * rhs.x
    infix fun scalar(f: Float) = Vec2(x * f, y * f)

    fun normalize(): Vec2 {
        val isr = FastMath.invSqrtFast(x * x + y * y)
        return Vec2(this.x * isr, this.y * isr)
    }

    override fun equals(obj: Any?): Boolean {
        if (obj == null) return false
        if (obj is Vec2) {
            val other = obj as Vec2?
            if (other!!.x == x && other.y == y)
                return true
        }
        return false
    }

    override fun toString(): String {
        return "<$x,$y>"
    }
}
