package spirite.base.imageData.deformation

import com.hackoeur.jglm.support.FastMath
import rb.vectrix.linear.Vec2f
import rb.vectrix.mathUtil.MathUtil
import rb.vectrix.mathUtil.d
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
    val fromLen = min(fromX.size, fromY.size)

    val toX : FloatArray = FloatArray(fromLen) {Float.MIN_VALUE}
    val toY : FloatArray = FloatArray(fromLen) {Float.MIN_VALUE}
    init {
        // Note: the from-to logic could either be brought out earlier, made more complicated, etc
        // for now it's designed to treat the lengths equally
        val toLen = min(toX.size, toY.size)

        for (i in (0 until toLen)) {
            val toIndex = MathUtil.clip(0, (i.f / toLen.f * fromLen.f).round, toLen)

            this.toX[toIndex] = toX[i]
            this.toY[toIndex] = toY[i]
        }

        // Populate missing segments
        var fillerX = this.toX.firstOrNull { it != Float.MIN_VALUE } ?: 0f
        var fillerY = this.toY.firstOrNull { it != Float.MIN_VALUE } ?: 0f

        for( i in (0 until fromLen)) {
            if( this.toX[i] == Float.MIN_VALUE) this.toX[i] = fillerX
            else fillerX = this.toX[i]

            if( this.toY[i] == Float.MIN_VALUE) this.toY[i] = fillerY
            else fillerY = this.toY[i]
        }

    }
}

class StrokeDeformation(
        val pieces : List<StrokeDeformationPiece>) : IDeformation
{
    override fun transform(x: Float, y: Float) : Vec2f {
        val numMutations = pieces.sumBy { it.fromLen }
        val weights = DoubleArray(numMutations)

        var offset = 0
        for (piece in pieces) {
            for (i in 0 until piece.fromLen) {
                weights[i + offset] = 1/MathUtil.distance( x.d, y.d, piece.fromX[i].d, piece.fromY[i].d)
            }

            offset += piece.fromLen
        }

        val sumWeights = weights.sum()

        var newX = x
        var newY = y
        offset = 0
        for (piece in pieces) {
            for (i in 0 until piece.fromLen) {
                newX += ((piece.toX[i] - piece.fromX[i]) * weights[i + offset]/sumWeights).f
                newY += ((piece.toY[i] - piece.fromY[i]) * weights[i + offset]/sumWeights).f
            }
            offset += piece.fromLen
        }

        if( MathUtil.distance(x, y, newX, newY) > 30) {
            println("BAD")
        }

        return Vec2f(newX, newY)
    }
}