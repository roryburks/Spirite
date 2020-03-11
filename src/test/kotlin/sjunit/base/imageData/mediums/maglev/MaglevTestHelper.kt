package sjunit.base.imageData.mediums.maglev

import rb.vectrix.mathUtil.MathUtil
import rb.vectrix.mathUtil.f
import rb.vectrix.mathUtil.floor
import spirite.base.imageData.mediums.magLev.MaglevStroke
import spirite.base.pen.stroke.DrawPoints
import spirite.base.pen.stroke.StrokeParams

object MaglevTestHelper {
    fun makeMaglevLine(x1: Float, y1: Float, x2: Float, y2: Float): MaglevStroke
    {
        val params = StrokeParams()
        val len = MathUtil.distance(x1,y1,x2,y2)
        val n = len.floor + 1
        val points=  DrawPoints(
                (0..n).map { MathUtil.lerp(x1, x2, it.f / n.f) }.toFloatArray(),
                (0..n).map { MathUtil.lerp(y1, y2, it.f / n.f) }.toFloatArray(),
                (0..n).map { 1f }.toFloatArray())
        return MaglevStroke(params, points)
    }
}