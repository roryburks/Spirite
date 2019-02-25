package spirite.base.imageData.mediums.magLev

import rb.vectrix.linear.Vec2f
import spirite.base.imageData.mediums.BuiltMediumData
import spirite.base.pen.stroke.DrawPoints
import spirite.base.pen.stroke.StrokeParams

class MaglevStroke(
        val params: StrokeParams,
        val drawPoints: DrawPoints)
    : IMaglevThing
{
    override fun transformPoints(lambda: (Vec2f) -> Vec2f) {
        for( i in 0 until drawPoints.length) {
            val vec = lambda(Vec2f(drawPoints.x[i], drawPoints.y[i]))
            drawPoints.x[i] = vec.xf
            drawPoints.y[i] = vec.yf
        }
    }

    override fun draw(built: BuiltMediumData) {
        val drawer = built.arranged.handle.workspace.strokeProvider.getStrokeDrawer(params)
        built.rawAccessComposite {
            drawer.batchDraw(it.graphics, drawPoints, params, built.width, built.height)
        }
    }

    override fun dupe() = MaglevStroke(params, drawPoints.dupe())
}