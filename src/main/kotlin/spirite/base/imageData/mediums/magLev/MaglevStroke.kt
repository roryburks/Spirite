package spirite.base.imageData.mediums.magLev

import rb.glow.IGraphicsContext
import rb.vectrix.linear.Vec3f
import rb.glow.SColor
import spirite.base.imageData.mediums.BuiltMediumData
import spirite.base.pen.stroke.DrawPoints
import spirite.base.pen.stroke.IStrokeDrawerProvider
import spirite.base.pen.stroke.StrokeParams

class MaglevStroke(
        var params: StrokeParams,
        val drawPoints: DrawPoints)
    : IMaglevThing,
        IMaglevPointwiseThing, IMaglevColorwiseThing
{
    override fun transformColor(lambda: (SColor) -> SColor) : Boolean{
        val oldColor = params.color
        val newColor = lambda(oldColor)
        if( oldColor != newColor) {
            params = params.copy(color = newColor)
            return true
        }
        return false
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
        built.rawAccessComposite {
            draw(it.graphics, built.arranged.handle.workspace.strokeProvider, built.width, built.height)
        }
    }

    fun draw(gc: IGraphicsContext, strokeProvider: IStrokeDrawerProvider, width: Int, height:Int) {
        println("draw")
        strokeProvider.getStrokeDrawer(params).batchDraw(gc, drawPoints, params,width, height)
    }

    override fun dupe() = MaglevStroke(params, drawPoints.dupe())
}