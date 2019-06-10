package rb.vectrix.intersect

import rb.vectrix.shapes.*
import kotlin.math.PI


sealed class CollisionObject
{
    abstract val bounds: Rect
    infix fun intersects(other: CollisionObject) = bounds.intersects(other.bounds) && _intersects(other)

    protected abstract fun _intersects(other: CollisionObject) : Boolean
}

data class CollisionPoint(val x: Double, val y: Double) : CollisionObject() {
    override val bounds: Rect get() = RectD(x, y, 0.0, 0.0)

    override fun _intersects(other: CollisionObject) = this intersectsPrecise  other
}

class CollisionRigidRect(val rect: Rect) : CollisionObject() {
    override val bounds: Rect get() = rect

    override fun _intersects(other: CollisionObject) = rect intersectsPrecise other
}

class CollisionCircle(val circle: Circle) : CollisionObject() {
    override val bounds: Rect by lazy { RectD(circle.x - circle.r, circle.y-circle.r, 2*circle.r, 2*circle.r) }

    override fun _intersects(other: CollisionObject): Boolean = circle intersectsPrecise other != null
}

class CollisionArc(val arc: Arc) : CollisionObject(){

    override val bounds: Rect by lazy {
        val x1 = minOf(arc.xStart, arc.xEnd, if(arc.inRange(PI)) arc.x-arc.r else arc.x)
        val y1 = minOf(arc.yStart, arc.yEnd, if(arc.inRange(3*PI/2)) arc.y-arc.r else arc.y)
        val x2 = maxOf(arc.xStart, arc.xEnd, if(arc.inRange(0.0)) arc.x+arc.r else arc.x)
        val y2 = minOf(arc.yStart, arc.yEnd, if(arc.inRange(PI/2)) arc.y+arc.r else arc.y)
        RectD(x1, y1, x2, y2)
    }

    override fun _intersects(other: CollisionObject) = (arc intersectsPrecise other) != null
}

class CollisionLineSegment(val lineSegment: LineSegment) : CollisionObject() {
    override val bounds: Rect get() = RectD.FromEndpoints(lineSegment.x1, lineSegment.y1, lineSegment.x2, lineSegment.y2)

    override fun _intersects(other: CollisionObject) = (lineSegment intersectsPrecise other) != null
}

class CollisionRayRect( val rayRect: RayRect) : CollisionObject() {
    override val bounds: Rect by lazy { RectD.FromPoints(rayRect.points.asSequence()) }

    override fun _intersects(other: CollisionObject) = (rayRect intersectsPrecise other) != null
}

class CollisionParabola( val parabola: Parabola) : CollisionObject() {
    override val bounds: Rect get() = RectD.FromEndpoints(parabola.x1, parabola.yB, parabola.x2, parabola.yT)

    override fun _intersects(other: CollisionObject) = parabola intersectsPrecise other

}

class CollisionPolygon( val polygon: IPolygon) : CollisionObject() {
    override val bounds: Rect by lazy {
        val p = polygon.vertices.asSequence()
        RectD.FromEndpoints(
            p.map { it.x }.min() ?: 0.0,
            p.map { it.y }.min() ?: 0.0,
            p.map { it.x }.max() ?: 0.0,
            p.map { it.y }.max() ?: 0.0)
    }


    override fun _intersects(other: CollisionObject) = polygon intersectsPrecise other
}


class CollisionMultiObj(val objs: List<CollisionObject>) : CollisionObject() {
    override val bounds: Rect by lazy { RectD.FromEndpoints(
        objs.asSequence().map { it.bounds.x1 }.min() ?: 0.0,
        objs.asSequence().map { it.bounds.y1 }.min() ?: 0.0,
        objs.asSequence().map { it.bounds.x2 }.max() ?: 0.0,
        objs.asSequence().map { it.bounds.y2 }.max() ?: 0.0) }

    override fun _intersects(other: CollisionObject) = objs.any{ it intersects other}
}

