package spirite.base.util.linear

/**
 * Created by Rory Burks on 4/28/2017.
 */

data class Vec2i (
        val x: Int,
        val y: Int
){
    constructor(v: Vec2i): this(v.x, v.y) {}

    operator fun minus(rhs: Vec2i) = Vec2i(x - rhs.x, y - rhs.y)
    operator fun plus(rhs: Vec2i) = Vec2i(x + rhs.x, y + rhs.y)

    infix fun dot(rhs: Vec2i) = this.x * rhs.x + this.y * rhs.y

    override fun equals(obj: Any?): Boolean {
        if (obj == null) return false
        if (obj is Vec2i) {
            val other = obj as Vec2i?
            if (other!!.x == x && other.y == y)
                return true
        }
        return false
    }
}
