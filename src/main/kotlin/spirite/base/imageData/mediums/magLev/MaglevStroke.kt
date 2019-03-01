package spirite.base.imageData.mediums.magLev

import rb.vectrix.linear.Vec2f
import rb.vectrix.linear.Vec3
import rb.vectrix.linear.Vec3f
import spirite.base.imageData.mediums.BuiltMediumData
import spirite.base.pen.stroke.DrawPoints
import spirite.base.pen.stroke.StrokeParams
import spirite.pc.gui.SColor

class MaglevStroke(
        var params: StrokeParams,
        val drawPoints: DrawPoints)
    : IMaglevThing,
        IMaglevPointwiseThing, IMaglevColorwiseThing
{
    override fun transformColor(lambda: (SColor) -> SColor) {
        val oldColor = params.color
        val newColor = lambda(oldColor)
        if( oldColor != newColor) {
            params = params.copy(color = newColor)
        }
    }

    override fun transformPoints(lambda: (Vec3f) -> Vec3f) {
        for( i in 0 until drawPoints.length) {
            val vec = lambda(Vec3f(drawPoints.x[i], drawPoints.y[i], drawPoints.w[i]))
            drawPoints.x[i] = vec.xf
            drawPoints.y[i] = vec.yf
            drawPoints.w[i] = vec.zf
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