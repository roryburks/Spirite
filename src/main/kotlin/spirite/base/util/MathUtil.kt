package spirite.base.util

import rb.vectrix.linear.Vec2f

/**point into two coordinates: the first representing
 * its projection onto the line segment normalized such that t=0 means it's perpendicular
 * to (x1,y1) and t=1 for (x2,y2).  The second representing the distance from the line
 * extended from the line segment
 */
fun projectOnto(x1: Float, y1: Float, x2: Float, y2: Float, p: Vec2f): Vec2f {
    val b = Vec2f(x2 - x1, y2 - y1)
    val scale_b = b.mag
    val scale_b2 = scale_b * scale_b

    val a = Vec2f(p.xf - x1, p.yf - y1)

    val t = a.dot(b) / scale_b2    // the extra / ||b|| is to normalize it to ||b|| = 1
    val m = a.cross(b) / scale_b

    return Vec2f(t, m)
}