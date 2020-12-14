package rb.glow

import rb.vectrix.mathUtil.d
import rb.vectrix.mathUtil.f
import rb.vectrix.shapes.*

interface IDrawingLayer {
    // Draw
    fun drawRect(x: Double, y: Double, w: Double, h: Double)
    fun drawRect(rect: Rect) = drawRect(rect.x1, rect.y1, rect.w, rect.h)
    fun drawOval(x: Double, y: Double, w: Double, h: Double) = draw(OvalShape((x +  w/2).f, (y + h/2).f, (w/2).f, (h/2).f ))
    fun drawLine(x1: Double, y1: Double, x2: Double, y2: Double)
    fun drawLine( lineSegment: LineSegment) = drawLine(lineSegment.x1, lineSegment.y1, lineSegment.x2, lineSegment.y2)
    fun draw( shape: IShape)

    // Fill
    fun fillRect(x: Double, y: Double, w: Double, h: Double)
    fun fillRect(rect: Rect) = fillRect(rect.x1, rect.y1, rect.w, rect.h)
    fun fillOval(x: Double, y: Double, w: Double, h: Double) = fill(
            OvalShape(x.f + w.f / 2.0f, y.f + h.f / 2.0f, w.f / 2.0f, h.f / 2.0f))
    fun fill( shape: IShape)
    fun fillPolygon( polygon: IPolygon)
}

val IGraphicsContext.drawer get() = DrawingLayer(this)

class DrawingLayer( val gc: IGraphicsContext)
    : IDrawingLayer
{
    override fun drawRect(x: Double, y: Double, w: Double, h: Double) {
        val x_ = doubleArrayOf(x, x+w, x+w, x).asIterable()
        val y_ = doubleArrayOf(y, y, y+h, y+h).asIterable()
        gc.drawPolyLine(x_, y_, 4, true)
    }

    override fun drawLine(x1: Double, y1: Double, x2: Double, y2: Double) {
        val x_ = doubleArrayOf(x1, x2).asIterable()
        val y_ = doubleArrayOf(y1, y2).asIterable()
        gc.drawPolyLine(x_, y_, 2, false)
    }

    override fun draw(shape: IShape) {
        val x_y = shape.buildPath(0.5f)
        gc.drawPolyLine(
            x_y.first.map { it.d },
            x_y.second.map { it.d },
            x_y.first.size,
            true)
    }

    override fun fillRect(x: Double, y: Double, w: Double, h: Double) {
        val x_ = doubleArrayOf(x, x+w, x+w, x).asIterable()
        val y_ = doubleArrayOf(y, y, y+h, y+h).asIterable()
        gc.fillPolygon(x_, y_, 4)
    }

    override fun fill(shape: IShape) {
        val x_y = shape.buildPath(0.5f)
        gc.fillPolygon(
            x_y.first.map { it.d },
            x_y.second.map { it.d },
            x_y.first.size)
    }

    override fun fillPolygon(polygon: IPolygon) {
        gc.fillPolygon(polygon.vertices.map { it.x }, polygon.vertices.map { it.y }, polygon.vertices.size)
    }
}
