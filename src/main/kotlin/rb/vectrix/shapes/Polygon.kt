package rb.vectrix.shapes

import rb.vectrix.linear.Vec2
import rb.vectrix.linear.Vec2d

interface IPolygon
{
    val vertices : List<Vec2>
    val sides : List<LineSegment>
}

/** Unvalidated */
data class DirectPolygon(override val vertices: List<Vec2>, override val sides: List<LineSegment>): IPolygon

data class PolygonD
private constructor(override val vertices: List<Vec2d>, override val sides: List<LineSegmentD>)
    :IPolygon
{
    companion object {
        fun Make( points: List<Vec2>) : PolygonD {
            val lines = List(points.size) {
                val point1 = points[it]
                val point2 = if( it == points.size - 1) points[0] else points[it+1]

                LineSegmentD(point1.x, point1.y, point2.x, point2.y)
            }
            return PolygonD(points.map { Vec2d(it.x, it.y) }, lines)
        }

        // TODO: Validation
        fun Make(vertices: List<Vec2>, sides: List<LineSegment>) =
            PolygonD(
                vertices.map { Vec2d(it.x, it.y) },
                sides.map { LineSegmentD(it.x1, it.y1, it.x2, it.y2) })
    }
}