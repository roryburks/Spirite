package spirite.base.imageData.deformation

import rb.vectrix.linear.Vec2f
import rb.vectrix.mathUtil.MathUtil
import rb.vectrix.mathUtil.f
import rb.vectrix.mathUtil.round
import kotlin.math.min

interface IDeformation {
    fun transform(x: Float, y: Float) : Vec2f
}

class StrokeDeformationPiece(
        val fromX : FloatArray,
        val fromY : FloatArray,
        toX : FloatArray,
        toY : FloatArray)
{
    val len = min(fromX.size, fromY.size)

    val toX : FloatArray = FloatArray(len) {Float.MIN_VALUE}
    val toY : FloatArray = FloatArray(len) {Float.MIN_VALUE}
    init {
        // Note: the from-to logic could either be brought out earlier, made more complicated, etc
        // for now it's designed to treat the lengths equally
        val toLen = min(toX.size, toY.size)

        for (i in (0 until toLen)) {
            val toIndex = MathUtil.clip(0, (i.f / toLen.f * len.f).round, toLen-1)

            this.toX[toIndex] = toX[i]
            this.toY[toIndex] = toY[i]
        }

        // Populate missing segments
        var fillerX = this.toX.firstOrNull { it != Float.MIN_VALUE } ?: 0f
        var fillerY = this.toY.firstOrNull { it != Float.MIN_VALUE } ?: 0f

        for( i in (0 until len)) {
            if( this.toX[i] == Float.MIN_VALUE) this.toX[i] = fillerX
            else fillerX = this.toX[i]

            if( this.toY[i] == Float.MIN_VALUE) this.toY[i] = fillerY
            else fillerY = this.toY[i]
        }

    }
}

