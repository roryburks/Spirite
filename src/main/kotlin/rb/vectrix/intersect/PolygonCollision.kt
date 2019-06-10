package rb.vectrix.intersect

import rb.vectrix.shapes.*

/*** NOTE:  All Polygon related collision assumes that the Polygon is "well-formed", meaning that its LineSegments
 * constitutes a single contiguous cycle and its points are exactly the points on the edges of each line segment.
 *
 * If these assumptions do not hold, the collision detection will not be correct. */

infix fun IPolygon.intersectsPrecise(obj: CollisionObject) : Boolean = when(obj) {
    is CollisionPoint -> PolygonCollision.withPoint(this, obj.x, obj.y)
    is CollisionLineSegment -> PolygonCollision.withLineSegment(this, obj.lineSegment)
    is CollisionRigidRect -> PolygonCollision.withRigidRect(this, obj.rect)
    is CollisionRayRect -> PolygonCollision.withRayRect(this, obj.rayRect)
    is CollisionCircle -> PolygonCollision.withCircle(this, obj.circle)
    is CollisionArc -> PolygonCollision.withArc(this, obj.arc)
    is CollisionParabola -> PolygonCollision.withParabola(this, obj.parabola)
    is CollisionPolygon -> PolygonCollision.withPolygon(this, obj.polygon)
    is CollisionMultiObj -> obj.objs.any() { this intersectsPrecise it }
}

object PolygonCollision {
    fun withPoint(polygon: IPolygon, x: Double, y: Double) : Boolean {
        // Odd-pass logic
        val roll = polygon.sides
            .count { _singleSideBoundedLineCollision(x,y, 0.0, it) }

        return (roll % 2) == 1
    }

    fun withLineSegment(polygon: IPolygon, line: LineSegment) : Boolean {
        if( polygon.sides.any {it intersection line != null })
            return true

        if( withPoint(polygon, line.x1, line.y1))
            return true

        return false
    }

    fun withRigidRect(polygon: IPolygon, rect: Rect) : Boolean =
            withPolygon(polygon, DirectPolygon(rect.points, rect.lineSegments))

    fun withRayRect(polygon: IPolygon, rayRect: RayRect) : Boolean =
            withPolygon(polygon, DirectPolygon(rayRect.points, rayRect.lineSegments))

    fun withCircle(polygon: IPolygon, circle: Circle) : Boolean {
        val first = polygon.vertices.firstOrNull() ?: return false
        if( CircleCollision.withPoint(circle, first.x, first.y) != null) return true
        if( withPoint(polygon, circle.x, circle.y)) return true

        return polygon.sides.any { CircleCollision.withLineSegment(circle, it) != null }
    }

    fun withArc(polygon: IPolygon, arc: Arc) : Boolean {
        val first = polygon.vertices.firstOrNull() ?: return false
        if( ArcCollision.withPoint(arc, first.x, first.y) != null) return true
        if( withPoint(polygon, arc.x, arc.y)) return true

        return polygon.sides.any { ArcCollision.withLineSegment(arc, it) != null }
    }

    fun withParabola(polygon: IPolygon, parabola: Parabola) : Boolean {
        val first = polygon.vertices.firstOrNull() ?: return false
        // Really not all that important to do the point collision as since the Parabola has no volume, it should be caught

        return polygon.sides.any { ParabolaCollision.withLineSegment(parabola, it) }
    }

    fun withPolygon( poly1: IPolygon, poly2: IPolygon) : Boolean {
        // Either one polygon is completely inside the other or they have some line segment collision
        val first1 = poly1.vertices.firstOrNull() ?: return false
        val first2 = poly2.vertices.firstOrNull() ?: return false
        if( withPoint(poly1, first2.x, first2.y)) return true
        if( withPoint(poly2, first1.x, first1.y)) return true

        return poly1.sides
            .any{ it1 -> poly2.sides.any { it2 -> it1 intersection  it2 != null }}
    }

    /*** Intersection between a lineSegment and the points (x + c*m, y + c*m) where c in [0,infinity)*/
    private fun _singleSideBoundedLineCollision(x: Double, y: Double, m: Double, line: LineSegment) : Boolean {
        if( line.x1 == line.x2) {
            return  line.x1 >= x && y in line.bottom..line.top
        }

        val m1 = m
        val m2 = line.m

        if( m1 == m2) return false

        val b1 = y
        val b2 = line.b
        val xc = (b2-b1)/(m1-m2)

        if( xc !in line.left..line.right) return false
        // probably not right for -m, but for now only using m=0
        return  xc >= x
    }
}