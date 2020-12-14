package rb.vectrix.shapes.helpers

import rb.vectrix.linear.Vec2d
import rb.vectrix.shapes.Circle
import rb.vectrix.shapes.IPolygon
import rb.vectrix.shapes.PolygonD
import kotlin.math.*

fun Circle.ToPoly(maxDelta: Double) : IPolygon {
    val inner = sqrt(2* abs(maxDelta) / abs( this.r ))
    val thDelta = when {
        inner <= 0 -> PI/180.0
        inner >= 1 -> PI
        else -> 2*asin(inner)
    }

    val points = mutableListOf<Vec2d>()
    var theta = 0.0
    while( theta < 2* PI)
    {
        points.add(Vec2d(x + cos(theta)*r, y + sin(theta)*r))
        theta += thDelta
    }

    return PolygonD.Make(points)
}