package spirite.base.util.linear

import kotlin.math.max
import kotlin.math.min

data class Rect(
        var x: Int,
        var y: Int,
        var width: Int,
        var height: Int
) {

    val isEmpty: Boolean get() = width <= 0 || height <= 0

    constructor(width: Int, height: Int): this(0,0,width,height) {}

    constructor(other: Rect) :this(other.x,other.y,other.width,other.height){}

    constructor(v: Vec2i) : this( 0, 0, v.x, v.y) {}

    infix fun intersection(other: Rect): Rect {
        val x1 = Math.max(x, other.x)
        val y1 = Math.max(y, other.y)
        val x2 = Math.min(x + width, other.x + other.width)
        val y2 = Math.min(y + height, other.y + other.height)
        return Rect(x1, y1, x2 - x1, y2 - y1)
    }

    fun contains(x2: Int, y2: Int): Boolean {
        return if (width <= 0 || height <= 0) false else !(x2 < x || y2 < y || x2 > x + width || y2 > y + height)
    }

    operator fun contains(rect: Rect): Boolean {
        if (width or height or rect.width or rect.height < 0) return false
        if (rect.x < this.x || rect.y < this.y) return false
        val x2 = x + width
        val X2 = rect.x + rect.width
        if (X2 < rect.x) {
            if (x2 >= x || X2 > x2) return false
        } else {
            if (x2 >= x && X2 > x2) return false
        }
        val y2 = y + height
        val Y2 = rect.y + rect.height
        if (Y2 < rect.y) {
            if (y2 >= y || Y2 > y2) return false
        } else {
            if (y2 >= y && Y2 > y2) return false
        }
        return true
    }

    infix fun union(rect: Rect?): Rect {
        return when {
            rect == null || rect.isEmpty -> Rect(this)
            isEmpty -> Rect(rect)
            else -> {
                val rx1 = min(x,rect.x)
                val ry1 = min(y,rect.y)
                val rx2 = max(x + width, rect.x + rect.width)
                val ry2 = max(y + height, rect.y + rect.height)
                Rect( rx1, ry1, rx2-rx1, ry2-ry1)
            }
        }
    }

    fun union(x: Int, y: Int, w: Int, h: Int): Rect {
        return union(Rect(x, y, w, h))
    }

    infix fun intersects(r: Rect): Boolean {
        var tw = this.width
        var th = this.height
        var rw = r.width
        var rh = r.height
        if (rw <= 0 || rh <= 0 || tw <= 0 || th <= 0) {
            return false
        }
        val tx = this.x
        val ty = this.y
        val rx = r.x
        val ry = r.y
        rw += rx
        rh += ry
        tw += tx
        th += ty
        //      overflow || intersect
        return (rw < rx || rw > tx) &&
                (rh < ry || rh > ty) &&
                (tw < tx || tw > rx) &&
                (th < ty || th > ry)
    }

    override fun toString() = "($x,$y),[$width x $height]"
}
