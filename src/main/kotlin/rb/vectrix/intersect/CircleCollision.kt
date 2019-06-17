package rb.vectrix.intersect

import rb.vectrix.linear.Vec2
import rb.vectrix.linear.Vec2d
import rb.vectrix.mathUtil.MathUtil
import rb.vectrix.shapes.*
import kotlin.math.*

infix fun Circle.intersectsPrecise(other: CollisionObject) : Double? = when(other) {
    is CollisionPoint -> CircleCollision.withPoint(this, other.x, other.y)
    is CollisionLineSegment -> CircleCollision.withLineSegment(this, other.lineSegment)
    is CollisionRigidRect -> CircleCollision.withRigidRect(this, other.rect)
    is CollisionRayRect -> CircleCollision.withRayRect(this, other.rayRect)
    is CollisionCircle -> CircleCollision.withCircle(this, other.circle)
    is CollisionArc -> CircleCollision.withArc(this, other.arc)
    is CollisionParabola -> CircleCollision.withParabola(this, other.parabola)
    is CollisionPolygon -> CircleCollision.withPolygon(this, other.polygon)
    is CollisionMultiObj -> other.objs.asSequence().mapNotNull { this intersectsPrecise  it }.min()
}

object CircleCollision {
    private const val Error = 0.0001f

    fun Circle.roll(dist: Double) = if( dist > r) null else dist/r

    fun withPoint( circle: Circle, x: Double, y: Double) : Double? {
        val d = MathUtil.distance(circle.x, circle.y, x, y)
        return if( d <= circle.r) d / circle.r else null
    }

    fun withLineSegment( circle: Circle, line: LineSegment) : Double? {
        val normal = line.normal
        val a = Vec2d(line.x1 - circle.x, line.y1 - circle.y)
        val projection = a dot normal

        if( abs(projection) > circle.r) return null
        if( circle.x + projection*normal.x in line.left..line.right && circle.y + projection*normal.y in line.bottom..line.top)
            return abs(projection)/circle.r

        val d1 = MathUtil.distance(circle.x, circle.y, line.x1, line.y1)
        val d2 = MathUtil.distance(circle.x, circle.y, line.x2, line.y2)
        val d = minOf(d1, d2)

        return if( d <= circle.r) d/circle.r
        else null
    }

    fun withRigidRect(circle: Circle, rect: Rect): Double? {
        // Needs Testing
        val rx1 = rect.x1
        val rx2 = rect.x2
        val cx = circle.x
        val ry1 = rect.y1
        val ry2 = rect.y2
        val cy = circle.y
        val r = circle.r

        if( rect.contains(circle.x, circle.y))
            return 0.0

        return when {
            rx1 > cx -> when {
                rx1 > cx + r -> null
                ry1 > cy -> circle.roll(MathUtil.distance(cx, cy, rx1, ry1))
                ry2 < cy -> circle.roll(MathUtil.distance(cx,cy,rx1,ry2))
                else -> circle.roll(rx1 - cx)
            }
            rx2 < cx -> when {
                rx2 < cx - r -> null
                ry1 > cy -> circle.roll(MathUtil.distance(cx, cy, rx2, ry1))
                ry2 < cy -> circle.roll(MathUtil.distance(cx, cy, rx2, ry2))
                else -> circle.roll(cx - rx2)
            }
            else -> when {
                ry1 > cy -> circle.roll(ry1 - cy)
                ry2 < cy -> circle.roll(cy - ry2)
                else -> 0.0
            }
        }
    }

    fun withRayRect(circle: Circle, rect: RayRect) : Double? {
        if( RayRectCollision.withPoint(rect, circle.x, circle.y) != null)
            return 0.0

        val points = rect.points.asSequence()
            .mapNotNull { withPoint(circle, it.x, it.y) }
            .min()

        val lines = rect.lineSegments.asSequence()
            .mapNotNull { withLineSegment(circle, it) }
            .min()

        return MathUtil.minOrNull(points, lines)
    }

    fun withCircle( circle: Circle, other: Circle) : Double? {
        val dist = MathUtil.distance(circle.x, circle.y, other.x, other.y) - other.r
        return when {
            dist > circle.r -> null
            dist < 0 -> 0.0
            else -> 1 - (dist / circle.r)
        }
    }

    fun withArc( circle: Circle, arc: Arc) : Double? {
        if( ArcCollision.withPoint(arc, circle.x, circle.y) != null)
            return 0.0

        val dist =  MathUtil.distance(circle.x, circle.y, arc.x, arc.y)
        if( dist > arc.r + circle.r) return null

        val point1 = withPoint(circle, arc.x, arc.y)
        val point2 = withPoint(circle, arc.xStart, arc.yStart)
        val point3 = withPoint(circle, arc.xEnd, arc.yEnd)
        val point = MathUtil.minOrNull(point1,point2,point3)

        val line1 = withLineSegment(circle, arc.line1)
        val line2 = withLineSegment(circle, arc.line2)
        val line = MathUtil.minOrNull(line1, line2)

        if( arc.inRange(atan2(circle.y - arc.y, circle.x - arc.y)))
        {
            val cDist = (dist - circle.r) / arc.r
            return MathUtil.minOrNull(point, line, cDist)
        }
        return MathUtil.minOrNull(point,line)
    }

    fun withParabola(circle: Circle, parabola: Parabola) : Double? {
        // Special Case 1: Circle is completely out of range of the Parabola
        if( circle.x - circle.r > parabola.x2 || circle.x + circle.r < parabola.x1) return null

        // Step one, simplify to the specific case of checking against point (0,0) by shifting the parabola
        val A = parabola.A
        val B = 2*parabola.A*circle.x + parabola.B
        val C = parabola.A*circle.x*circle.x + parabola.B*circle.x + parabola.C - circle.y

        // https://math.stackexchange.com/questions/1520972/finding-the-shortest-distance-between-an-arbitrary-point-and-a-parabola
        // https://www.wolframalpha.com/input/?i=0+%3D+2+(x+%2B+(B+%2B+2+A+x)+(C+%2B+x+(B+%2B+A+x)))
        //
        // Basic outline:
        //  -Find the minimum distance between the simplified parabola and the point 0,0  by using the derivative of the distance function
        //  -Make sure the x from the minumum for the solution of a Pure Parabola (unbounded) is in range.
        //  -If it's not in range
        //
        // Alternately https://www.wolframalpha.com/input/?i=solve+%5Bx%5E2%2By%5E2%3Dr%5E2,a*x%5E2%2Bb*x%3Dy%5D+for+x,y
        // for precise points
        // have fun with that one

        val A2 = A*A
        val A3 = A2*A
        val A6 = A3*A3
        val B2 = B*B

        val underSqrt = 291*A6*B2 + 4*(12*A3*C - 3*A2*B2 + 6*A2).pow(3)
        if( underSqrt < 0) return null
        val cuberoot = (54*A3*B + sqrt(underSqrt)).pow(1/3.0)
        if( cuberoot == 0.0) return null

        val one = cuberoot / (6*(2.0.pow(1/3.0))*A2)
        val two = (12*A3*C-3*A2*B2 + 6*A2) / (3*(2.0.pow(2/3.0))*A2*cuberoot)
        val three = -B/(2*A)

        val xHat = one - two - three
        val x = xHat + circle.x
        if( x in parabola.x1..parabola.x2)
        {
            val yHat = A*xHat*xHat + B*xHat + C
            return circle.roll(sqrt(xHat*xHat + yHat*yHat))
        }

        val dist1 =
            if(parabola.x1 in (circle.x-circle.r)..(circle.x+circle.r))
                circle.roll(MathUtil.distance(circle.x, circle.y, parabola.x1, parabola.apply(parabola.x1)))
            else null
        val dist2 =
            if(parabola.x2 in (circle.x-circle.r)..(circle.x+circle.r))
                circle.roll(MathUtil.distance(circle.x, circle.y, parabola.x2, parabola.apply(parabola.x2)))
            else null
        return MathUtil.minOrNull(dist1, dist2)
    }

    fun withPolygon(circle: Circle, polygon: IPolygon) : Double? {
        if( PolygonCollision.withPoint(polygon, circle.x, circle.y))
            return 0.0

        return polygon.sides.asSequence()
            .mapNotNull { withLineSegment(circle, it) }
            .min()
    }

    fun lineIntersectionPoints(circle: Circle, line: LineSegment) : List<Vec2> {
        val rx1 = line.left - circle.x
        val rx2 = line.right - circle.x
        val ry1 = line.left - circle.y
        val ry2 = line.right - circle.y

        // http://mathworld.wolfram.com/Circle-LineIntersection.html
        val dx = rx2 - rx1
        val dy = ry2 - ry1
        val dr = MathUtil.distance(rx1, ry1, rx2, ry2)
        val D = rx1*ry2 - rx2*ry1
        val disc = circle.r*circle.r*dr*dr - D
        if( disc < 0) return emptyList()

        val sqrtDisc = sqrt(disc)

        val signdy = sign(dy)
        val drr = dr*dr

        return List(2) {
            val plusMinus = if(it == 1) 1 else -1

            val cx = (D*dy + plusMinus*signdy*dx*sqrtDisc)/drr
            when  {
                cx in rx1-Error..rx2+Error -> {
                    val cy = (-D*dx + plusMinus* abs(dy) *sqrtDisc)/drr
                    when {
                        cy in ry1-Error..ry2+Error -> Vec2d(cx,cy)
                        else -> null
                    }
                }
                else -> null
            }
        }.filterNotNull()
    }
}