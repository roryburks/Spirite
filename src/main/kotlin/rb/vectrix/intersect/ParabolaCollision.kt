package rb.vectrix.intersect

import rb.vectrix.shapes.LineSegment
import rb.vectrix.shapes.Parabola
import rb.vectrix.shapes.RayRect
import rb.vectrix.shapes.Rect
import kotlin.math.sqrt


// Could possibly return the X value?
infix fun Parabola.intersectsPrecise(obj: CollisionObject) : Boolean = when(obj) {
    is CollisionPoint -> ParabolaCollision.withPoint(this, obj.x, obj.y)
    is CollisionLineSegment -> ParabolaCollision.withLineSegment(this, obj.lineSegment)
    is CollisionRigidRect -> ParabolaCollision.withRect(this, obj.rect)
    is CollisionRayRect -> ParabolaCollision.withRayRect(this, obj.rayRect)
    is CollisionCircle -> CircleCollision.withParabola(obj.circle, this) != null
    is CollisionArc -> TODO()
    is CollisionParabola -> ParabolaCollision.withParabola(this, obj.parabola)
    is CollisionPolygon -> PolygonCollision.withParabola(obj.polygon, this)
    is CollisionMultiObj -> obj.objs.asSequence().any { this intersectsPrecise it }
}

object ParabolaCollision {
    private const val ERROR = 0.000001

    fun withPoint(parabola: Parabola, x: Double, y: Double) : Boolean {
        if( x !in parabola.x1..parabola.x2) return false

        return parabola.A*(x*x) + parabola.B*x + parabola.C in (y- ERROR)..(y+ ERROR)
    }

    fun withLineSegment(parabola: Parabola, lineSegment: LineSegment) : Boolean {
        val A = parabola.A
        val B = parabola.B
        val C = parabola.C
        if( lineSegment.x1 == lineSegment.x2) {
            val x = lineSegment.x1
            val y = A*x*x + B*x + C

            return y in lineSegment.y1..lineSegment.y2
        }

        val m = lineSegment.m
        val b = lineSegment.b

        val underSqrt = 4*A*b - 4*A*C + B*B + m*m - 2*B*m

        if( underSqrt < 0) return false

        val sqrt = sqrt(underSqrt)
        if( (-B + m + sqrt )/2*A in lineSegment.x1- ERROR..lineSegment.x2+ ERROR) return true
        if( (-B + m - sqrt )/2*A in lineSegment.x1- ERROR..lineSegment.x2+ ERROR) return true
        return false
    }

    fun withRect(parabola: Parabola, rect: Rect) : Boolean {
        if( rect.contains(parabola.x1, parabola.yB)) return true

        return rect.lineSegments.any { withLineSegment(parabola, it) }
    }

    fun withRayRect(parabola: Parabola, rect: RayRect) : Boolean {
        if( RayRectCollision.withPoint(rect, parabola.x1, parabola.yB) != null) return true
        return rect.lineSegments.any { withLineSegment(parabola, it) }
    }

    fun withParabola(parabola: Parabola, other: Parabola) : Boolean {
        // Quadradic Formula with (A1-A2)x^2 + (B1-B2)x + (C1-C2) = 0
        val A = parabola.A - other.A
        val B = parabola.B - other.B
        val C = parabola.C - other.C

        val underSqrt = B*B - 4*A*C
        if( underSqrt < 0)
            return false
        val sqrt = sqrt(underSqrt)

        val x1 = (-B + sqrt) / (2*A)
        if( x1 in parabola.x1..parabola.x2 && x1 in other.x1..other.x2)
            return true
        val x2  = (-B - sqrt)/ (2*A)
        if( x2 in parabola.x1..parabola.x2 && x2 in other.x1..other.x2)
            return true

        return false
    }

}