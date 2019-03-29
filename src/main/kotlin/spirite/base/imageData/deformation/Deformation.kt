package spirite.base.imageData.deformation

import com.hackoeur.jglm.support.FastMath
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
    val fromLen = min(fromX.size, fromY.size)

    val toX : FloatArray = FloatArray(fromLen)
    val toY : FloatArray = FloatArray(fromLen)
    init {
        // Note: the from-to logic could either be brought out earlier, made more complicated, etc
        // for now it's designed to treat the lengths equally
        val toLen = min(toX.size, toY.size)

        for (i in (0 until toLen)) {
            val toIndex = MathUtil.clip(0, (i.f / toLen.f * fromLen.f).round, toLen)

            this.toX[toLen] = toX[i]
            this.toY[toLen] = toY[i]
        }
    }
}


class StrokeDeformation(
        val pieces : List<StrokeDeformationPiece>) : IDeformation
{
    override fun transform(x: Float, y: Float) : Vec2f {
        val numMutations = pieces.sumBy { it.fromLen }
        val weights = FloatArray(numMutations)

        var offset = 0
        for (piece in pieces) {
            for (i in 0 until piece.fromLen) {
                weights[i + offset] = FastMath.invSqrtFast ((x - piece.fromX[i]).run { this*this } + (y - piece.fromY[i]).run { this*this })
            }

            offset += piece.fromLen
        }

        val sumWeights = weights.sum()

        var newX = x
        var newY = y
        offset = 0
        for (piece in pieces) {
            for (i in 0 until piece.fromLen) {
                newX += (piece.toX[i] - piece.fromX[i]) * weights[i + offset]/sumWeights
                newY += (piece.toY[i] - piece.fromY[i]) * weights[i + offset]/sumWeights
            }
            offset += piece.fromLen
        }
        return Vec2f(newX, newY)
    }
}