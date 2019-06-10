package rb.vectrix.intersect

import rb.vectrix.linear.Vec2
import rb.vectrix.mathUtil.MathUtil
import rb.vectrix.shapes.*
import kotlin.math.PI
import kotlin.math.abs

infix fun RayRect.intersectsPrecise(obj: CollisionObject) : Double? = when(obj) {
    is CollisionPoint -> RayRectCollision.withPoint(this, obj.x, obj.y)
    is CollisionLineSegment -> RayRectCollision.withLineSegment(this, obj.lineSegment)
    is CollisionRigidRect -> RayRectCollision.withRigidRect(this, obj.rect)
    is CollisionRayRect -> RayRectCollision.withRayRect(this, obj.rayRect)
    is CollisionCircle -> RayRectCollision.withCircle(this, obj.circle)
    is CollisionArc -> RayRectCollision.withArc(this, obj.arc)
    is CollisionParabola -> RayRectCollision.withParabola(this, obj.parabola)
    is CollisionPolygon -> RayRectCollision.withPolygon(this, obj.polygon)
    is CollisionMultiObj -> obj.objs.asSequence().mapNotNull { this intersectsPrecise it }.min()
}

object RayRectCollision {
    fun withPoint(rect: RayRect, x: Double, y: Double) : Double? {
        // By means of a short-circuiting implied Matrix Transformation
        val xproj = rect.projectX(x,y)
        if( xproj < 0 || xproj > rect.len) return null

        val yproj = rect.projectY(x,y)
        if( yproj < -rect.h/2 || yproj > rect.h/2) return null

        return xproj / rect.len
    }

    fun withLineSegment(rayRect: RayRect, line: LineSegment) : Double? {
        val c1 = withPoint(rayRect, line.x1, line.y1)
        val c2 = withPoint(rayRect, line.x2, line.y2)
        if( c1 != null && c2 != null) return minOf(c1, c2)


        val cback = rayRect.back intersection line
        if( cback != null) return 0.0

        val cside1 = rayRect.side1 intersection line
        val cside2 = rayRect.side2 intersection line

        return MathUtil.minOrNull(cside1, cside2)
    }

    fun withRigidRect( rayRect: RayRect, rect: Rect) =
        if( rect.contains(rayRect.x, rayRect.y)) 0.0
        else _withPolygon(rayRect, rect.lineSegments, rect.points)

    fun withRayRect( rayRect: RayRect, other: RayRect) =
        if( withPoint(other, rayRect.x, rayRect.y) != null) 0.0
        else _withPolygon(rayRect, listOf(other.back, other.front, other.side1, other.side2), other.points)

    fun withCircle(rayRect: RayRect, circle: Circle) : Double? {
        val cx = rayRect.projectX(circle.x, circle.y)
        val cy = rayRect.projectY(circle.x, circle.y)

        if( cy in -rayRect.h/2..rayRect.h/2) {
            return when {
                cx < -circle.r -> null
                cx > rayRect.len + circle.r -> null
                cx < circle.r -> 0.0
                else -> (cx - circle.r) / rayRect.len
            }
        }

        if( CircleCollision.withLineSegment(circle, rayRect.back) != null) return 0.0
        val side1 = CircleCollision.withLineSegment(circle, rayRect.side1)
        val side2 = CircleCollision.withLineSegment(circle, rayRect.side2)

        return MathUtil.minOrNull(side1, side2)
    }

    fun withArc( rayRect: RayRect, arc: Arc) : Double? {
        // Note: the problem is reduced WLOC to collision of an arc with a rectangle with theta = 0, x = 0, y = 0
        val ax = rayRect.projectX(arc.x, arc.y)
        val ay = rayRect.projectY(arc.x, arc.y)

        if( ay in -rayRect.h/2..rayRect.h/2) {
            // Only way for left-most or right-most circle to be the closest to the backline is if its y is in this range
            if (ax < 0 && arc.inRange(-rayRect.theta)) {
                // Back is hit
                return if (ax < arc.r) null else 0.0
            }
            if (ax >= 0 && arc.inRange(PI - rayRect.theta)) {
                // Essentially circle collision
                return if (ax > arc.r + rayRect.len) null else (ax - arc.r)  / rayRect.len
            }
        }

        // Back line intersection
        if( rayRect.back intersection arc.line1 ?: rayRect.back intersection arc.line2 != null) return 0.0

        val possibles = mutableListOf<Double?>()

        // Corner Points
        possibles.add(withPoint(rayRect, arc.x, arc.y))
        possibles.add(withPoint(rayRect, arc.xStart, arc.yStart))
        possibles.add(withPoint(rayRect, arc.xEnd, arc.yEnd))

        // Line Segment Intersections
        // (note: Still true that if a line segment crosses the front then it will necessarily have a closer or as-close intersection elsewhere)
        possibles.add(rayRect.side1 intersection  arc.line1)
        possibles.add(rayRect.side2 intersection  arc.line1)
        possibles.add(rayRect.side1 intersection  arc.line2)
        possibles.add(rayRect.side2 intersection  arc.line2)

        // Arc-LineSegment Intersection
        // (note: also true regarding arcs colliding with front)
        fun roll( line: LineSegment, point: Vec2) = when {
            abs(point.y - line.y1) > abs(point.x - line.x1) -> (point.y - line.y1) / (line.y2 - line.y1)
            else -> (point.x - line.x1) / (line.x2 - line.y1)
        }

        possibles.addAll(ArcCollision.arcSegmentWithLineSegment(arc, rayRect.side1).asSequence().map { roll(rayRect.side1, it) })
        possibles.addAll(ArcCollision.arcSegmentWithLineSegment(arc, rayRect.side2).asSequence().map { roll(rayRect.side2, it) })

        return possibles.asSequence()
            .filterNotNull()
            .min()
    }

    fun withParabola( rayRect: RayRect, parabola: Parabola) : Double? {
        // NOTE: This does not yet cover the case of the curve of the parabola being the closest thing to the back line

        // Back
        if( ParabolaCollision.withLineSegment(parabola,rayRect.back))
            return 0.0

        val withPoints = MathUtil.minOrNull(
            withPoint(rayRect,parabola.x1, parabola.apply(parabola.x1)),
            withPoint(rayRect, parabola.x2, parabola.apply(parabola.x2)))

        val withSides = MathUtil.minOrNull(
            LineSegmentCollision.withParabola(rayRect.side1, parabola),
            LineSegmentCollision.withParabola(rayRect.side2, parabola))

        return MathUtil.minOrNull(withPoints, withSides)
    }

    fun withPolygon( rayRect: RayRect, polygon: IPolygon) = _withPolygon(rayRect, polygon.sides, polygon.vertices)

    internal fun _withPolygon(rayRect: RayRect, sides: List<LineSegment>, vertices: List<Vec2>) : Double? {
        val intersectsBack = sides.asSequence()
            .mapNotNull { it intersection rayRect.back }
            .any()
        if( intersectsBack) return 0.0


        // If a collision happens in the front, one of the following are true:
        //   1: There is a collision in the back
        //   2: There is a collision on one of the sides
        //   3: One of the points of the Polygon is contained within the CollisionRect
        // In any of these three events, determining a front collision is irrelevant
//        val intersectsFront = lineSegments
//                .walkNotNull { rect.front intersection it }
//                .any()

        val closestPointIn = vertices.asSequence()
            .mapNotNull { withPoint(rayRect, it.x, it.y) }
            .min()
        val side1Intersections = sides.asSequence()
            .mapNotNull { rayRect.side1 intersection it }
            .min()
        val side2Intersections = sides.asSequence()
            .mapNotNull { rayRect.side2 intersection it }
            .min()

        return MathUtil.minOrNull(closestPointIn, side1Intersections, side2Intersections)

    }
}