package rb.glow.glu

import rb.glow.gle.GLPrimitive

interface IPolygonTesselator {
    fun tesselatePolygon(x: Sequence<Double>, y: Sequence<Double>, count: Int) : GLPrimitive
}