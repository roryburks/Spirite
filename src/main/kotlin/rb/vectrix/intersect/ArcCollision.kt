package rb.vectrix.intersect

import rb.vectrix.linear.Vec2
import rb.vectrix.mathUtil.MathUtil
import rb.vectrix.shapes.*
import kotlin.math.PI
import kotlin.math.atan2


infix fun Arc.intersectsPrecise(obj: CollisionObject) : Double? = when(obj) {
    is CollisionPoint -> ArcCollision.withPoint(this, obj.x, obj.y)
    is CollisionLineSegment -> ArcCollision.withLineSegment(this, obj.lineSegment)
    is CollisionRigidRect -> ArcCollision.withRect(this, obj.rect)
    is CollisionRayRect -> ArcCollision.withRayRect(this, obj.rayRect)
    is CollisionCircle -> ArcCollision.withCircle(this, obj.circle)
    is CollisionArc -> ArcCollision.withArc(this, obj.arc)
    is CollisionParabola -> TODO()
    is CollisionPolygon -> ArcCollision.withPolygon(this, obj.polygon)
    is CollisionMultiObj -> obj.objs.asSequence().mapNotNull { this intersectsPrecise it }.min()
}

object ArcCollision {
    fun withPoint(arc: Arc, x: Double, y: Double) : Double? {
        val dist = MathUtil.distance(arc.x, arc.y, x, y)
        return if(dist <= arc.r && arc.inRange(atan2(x - arc.y, y - arc.x)))
            dist/arc.r
        else null
    }

    fun withLineSegment(arc: Arc, line: LineSegment) : Double? {
        val point1 = withPoint(arc, line.x1, line.y1)
        val point2 = withPoint(arc, line.x2, line.y2)

        if( point1 != null && point2 != null)
            return minOf(point1, point2)

        val point = point1 ?: point2

        val line1: Double?
        val line2: Double?

        if( point != null) {
            line1 = arc.line1 intersection line
            if( line1 != null)
                return minOf(point, line1)
            line2 = arc.line2 intersection  line
            if( line2 != null)
                return minOf(point, line2)
        }
        else {
            line1 = arc.line1 intersection line
            line2 = arc.line2 intersection  line
            if( line1 != null && line2 != null)
                return minOf(line1, line2)
        }

        val arcCollision = arcSegmentWithLineSegment(arc, line)
        return when(arcCollision.size) {
            0 -> line1 ?: line2 ?: point
            2 -> MathUtil.distance(arc.x, arc.y, (arcCollision[0].x + arcCollision[1].x)/2, (arcCollision[0].y + arcCollision[1].y)/2) / arc.r
            else -> 1.0
        }
    }
    fun arcSegmentWithLineSegment( arc: Arc, line: LineSegment) =
        CircleCollision.lineIntersectionPoints(CircleD.Make(arc.x, arc.y, arc.r), line)
            .filter { arc.inRange(atan2(it.y-arc.y, it.x - arc.x)) }

    fun withRect(arc: Arc, rect: Rect) : Double? {
        // Perhaps not optimal, but good enough
        if (rect.contains(arc.x, arc.y))
            return 0.0

        return _withPolygon(arc, rect.lineSegments, rect.points)
    }

    fun withRayRect(arc: Arc, rect: RayRect) : Double? {
        if( RayRectCollision.withPoint(rect, arc.x, arc.y) != null)
            return 0.0

        return _withPolygon(arc, listOf(rect.back, rect.front, rect.side1, rect.side2), rect.points)
    }

    // Assumes that "Arc inside Polygon" has already been tested for
    internal fun _withPolygon(arc: Arc, lines: List<LineSegment>, points: List<Vec2>) : Double? {
        val minPoints = points.asSequence()
            .mapNotNull { withPoint(arc, it.x, it.y) }
            .min()

        val minLines = lines.asSequence()
            .mapNotNull {
                val cLine1 = arc.line1 intersection it
                val cLine2 = arc.line2 intersection it
                val cArc = arcSegmentWithLineSegment(arc, it)
                val cArcLen = when(cArc.size) {
                    0 -> null
                    2 -> {
                        val centerX = (cArc[0].x + cArc[1].x)/2
                        val centerY = (cArc[0].y + cArc[1].y)/2

                        val dir = atan2(centerY - arc.y, centerX - arc.x)
                        if( arc.inRange(dir)) MathUtil.distance(arc.x, arc.y, centerX, centerY)
                        else null
                    }
                    else -> 1.0
                }

                MathUtil.minOrNull(cArcLen, cLine1, cLine2)
            }
            .min()

        return MathUtil.minOrNull(minPoints, minLines)
    }

    fun withCircle(arc: Arc, circle: Circle): Double? {
        val dist = MathUtil.distance(arc.x, arc.y, circle.x, circle.y) - circle.r
        if (dist > arc.r) return null
        if (dist < 0) return 0.0

        val theta = atan2(circle.y - arc.y, circle.x - arc.x)
        if (arc.inRange(theta)) return 1 - (dist / arc.r)

        val lineSegment1 = LineSegmentD(arc.x, arc.y, arc.xStart, arc.yStart)
        val lineSegment2 = LineSegmentD(arc.x, arc.y, arc.xEnd, arc.yEnd)

        val intersection1 = LineSegmentCollision.withCircle(lineSegment1, circle)
        val intersection2 = LineSegmentCollision.withCircle(lineSegment2, circle)

        return MathUtil.minOrNull(intersection1, intersection2)
    }

    fun withArc(arc: Arc, other: Arc) : Double? {
        val dist = MathUtil.distance(arc.x, arc.y, other.x, other.y)
        if( dist > arc.r + other.r) return null
        val theta = atan2(other.y-arc.y, other.x - arc.x)
        if( dist < other.r && other.inRange(theta - PI)) return 0.0

        val possibles = mutableListOf<Double?>()

        // Possible 1: Center of other Arc
        if( arc.inRange(theta))
            possibles.add(dist / arc.r)

        // Possible 2: Closest point of Circle
        if( other.inRange(theta- PI))
            possibles.add(1 - ((dist - other.r)/arc.r))

        // Possible 3: StartPoint
        possibles.add(withPoint(arc,other.xStart, other.yStart))

        // Possible 4: EndPoint
        possibles.add(withPoint(arc,other.xEnd, other.yEnd))

        // Possible 5-8: Line Segment-LineSegment Intersection
        val thisSeg1 = LineSegmentD(arc.x, arc.y, arc.xStart, arc.yStart)
        val thisSeg2 = LineSegmentD(arc.x, arc.y, arc.xEnd, arc.yEnd)
        val otherSeg1 = LineSegmentD(other.x, other.y, other.xStart, other.yStart)
        val otherSeg2 = LineSegmentD(other.x, other.y, other.xEnd, other.yEnd)

        possibles.add( thisSeg1 intersection otherSeg1)
        possibles.add( thisSeg1 intersection otherSeg2)
        possibles.add( thisSeg2 intersection otherSeg1)
        possibles.add( thisSeg2 intersection otherSeg2)


        //(could possibly do some discrimination about whether this is necessary?)
        // Possible 9-12: Arc-LineSegment Intersection (our Arc)
        fun checkAndAddCenterPoint(points: List<Vec2>) {
            if( points.size < 2) return
            possibles.add(withPoint(arc, (points[0].x + points[1].x)/2, (points[0].y + points[1].y)/2))
        }
        val asCircle = CircleD.Make(arc.x, arc.y, arc.r)
        checkAndAddCenterPoint(CircleCollision.lineIntersectionPoints(asCircle, otherSeg1))
        checkAndAddCenterPoint(CircleCollision.lineIntersectionPoints(asCircle, otherSeg2))

        // Possible 13-16: Arc-LineSegment Intersection (their arc)
        val themCircle = CircleD.Make(other.x, other.y, other.r)
        possibles.addAll(CircleCollision.lineIntersectionPoints(themCircle, thisSeg1)
            .mapNotNull { withPoint(arc, it.x, it.y) })
        possibles.addAll(CircleCollision.lineIntersectionPoints(themCircle, thisSeg2)
            .mapNotNull { withPoint(arc, it.x, it.y) })

        return possibles.filterNotNull().min()
    }

    fun withPolygon(arc: Arc, polygon: IPolygon) : Double? {
        if( PolygonCollision.withPoint(polygon, arc.x, arc.y))
            return 0.0

        return polygon.sides.asSequence()
            .mapNotNull { withLineSegment(arc, it) }
            .min()
    }
}