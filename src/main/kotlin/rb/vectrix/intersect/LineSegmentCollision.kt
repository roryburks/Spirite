package rb.vectrix.intersect

import rb.vectrix.mathUtil.MathUtil
import rb.vectrix.shapes.*
import kotlin.math.abs
import kotlin.math.sqrt


infix fun LineSegment.intersectsPrecise(obj: CollisionObject) : Double? = when(obj) {
    is CollisionPoint -> LineSegmentCollision.withPoint(this, obj.x, obj.y)
    is CollisionLineSegment -> LineSegmentCollision.withLineSegment(this, obj.lineSegment)
    is CollisionRigidRect -> LineSegmentCollision.withRigidRect(this, obj.rect)
    is CollisionRayRect -> LineSegmentCollision.withRayRect(this, obj.rayRect)
    is CollisionCircle -> LineSegmentCollision.withCircle(this, obj.circle)
    is CollisionArc -> LineSegmentCollision.withArc(this, obj.arc)
    is CollisionParabola -> LineSegmentCollision.withParabola(this, obj.parabola)
    is CollisionPolygon -> LineSegmentCollision.withPolygon(this, obj.polygon)
    is CollisionMultiObj -> obj.objs.asSequence().mapNotNull { this intersectsPrecise it }.min()
}

object LineSegmentCollision {
    private const val Error = 0.0001f

    fun withPoint(line: LineSegment, x: Double, y: Double) : Double? {
        return when {
            x !in line.left..line.right -> null
            y !in line.bottom..line.top -> null
            line.x1 == line.x2 -> when {
                x == line.x1 -> (y-line.y1) / (line.y2-line.y1)
                else -> null
            }
            (y - line.y1) / (x - line.x1) !in line.m-Error..line.m+ Error -> null
            else -> (x - line.x1) / (line.x2 - line.x1)
        }
    }

    fun withLineSegment( line1: LineSegment, line2: LineSegment) = line1 intersection line2

    fun withRigidRect( line: LineSegment, rect: Rect) : Double? {
        if( rect.contains(line.x1, line.y1))
            return 0.0

        return rect.lineSegments.asSequence()
            .mapNotNull { line intersection it }
            .min()
    }

    fun withRayRect( line: LineSegment, rect: RayRect) : Double? {
        if( RayRectCollision.withPoint(rect, line.x1, line.y1) != null)
            return 0.0

        return rect.lineSegments.asSequence()
            .mapNotNull { line intersection it  }
            .min()
    }

    fun withCircle(line: LineSegment, circle: Circle) : Double? {
        val distStart = MathUtil.distance(circle.x, circle.y, line.x1, line.y1)
        if( distStart <= circle.r) {
            return 0.0
        }

        val rx1 = line.left - circle.x
        val rx2 = line.right - circle.x
        val ry1 = line.bottom - circle.y
        val ry2 = line.top - circle.y


        if( rx1 == rx2) {
            // Special Case.  Note: Could also do a special case of ry1=ry2
            val dx = abs(rx1)
            return when {
                dx > circle.r -> null
                dx == circle.r -> if( 0.0 in ry1..ry2) (-ry1)/(ry2-ry1) else null
                else -> {
                    val y = sqrt(circle.r*circle.r - rx1*rx1)
                    if( y in ry1..ry2) (y-ry1)/(ry2-ry1) else null
                }
            }
        }

        // http://mathworld.wolfram.com/Circle-LineIntersection.html

        val dx = rx2 - rx1
        val dy = ry2 - ry1
        val dr = MathUtil.distance(rx1, ry1, rx2, ry2)
        val D = rx1*ry2 - rx2*ry1
        val disc = circle.r*circle.r*dr*dr - D*D
        if( disc < 0) return null
        //if(disc == 0f) return ((D*dy/(dr*dr))- rx1) / (rx2-rx1)

        val sqrtDisc = sqrt(disc)

        val cx1 = (D*dy + dx*sqrtDisc)/(dr*dr)
        val cx2 = (D*dy - dx*sqrtDisc)/(dr*dr)

        val f1 = if( cx1 in rx1..rx2) (cx1-rx1)/(rx2-rx1) else null
        val f2 = if( cx2 in rx1..rx2) (cx2-rx1)/(rx2-rx1) else null

        return MathUtil.minOrNull(f1, f2)
    }

    fun withArc(line: LineSegment, arc: Arc) : Double? {
        if( ArcCollision.withPoint(arc, line.x1, line.y1) != null)
            return 0.0

        val colLine1 = line intersection arc.line1
        val colLine2 = line intersection arc.line2

        if( colLine1 != null && colLine2 != null)
            return minOf(colLine1, colLine2)

        val arcCollion = ArcCollision.arcSegmentWithLineSegment(arc, line)
            .asSequence()
            .map {
                if( abs(it.x - line.x1) > abs(it.y - line.y1)) (it.x - line.x1) / (line.x2 - line.x1)
                else it.y - line.y1 / (line.y2 - line.y1)
            }
            .min()

        return MathUtil.minOrNull(colLine1 ?: colLine2, arcCollion)
    }

    private const val ERROR = 0.000001
    fun withParabola( line: LineSegment, parabola: Parabola) : Double? {
        if( ParabolaCollision.withPoint(parabola, line.x1, line.y1))
            return 0.0


        val A = parabola.A
        val B = parabola.B
        val C = parabola.C
        if( line.x1 == line.x2) {
            val x = line.x1
            val y = A*x*x + B*x + C

            return if( y in line.y1..line.y2) (y - line.y1) / (line.y2 - line.y1)
                else null
        }


        val m = line.m
        val b = line.b

        val underSqrt = 4*A*b - 4*A*C + B*B + m*m - 2*B*m
        if( underSqrt < 0)
            return null

        val sqrt = sqrt(underSqrt)
        val x1 = (-B + m + sqrt )/2*A
        if( x1 in line.x1- ERROR..line.x2+ERROR) return (x1 - line.x1) / (line.x2 - line.x1)
        val x2 = (-B + m - sqrt )/2*A
        if( x2 in line.x1- ERROR..line.x2+ERROR) return (x2 - line.x1) / (line.x2 - line.x1)

        return null
    }

    fun withPolygon( line: LineSegment, polygon: IPolygon) : Double? {
        if( PolygonCollision.withPoint(polygon, line.x1, line.y1)) return 0.0

        return polygon.sides.asSequence()
            .mapNotNull { line intersection it }
            .min()
    }
}