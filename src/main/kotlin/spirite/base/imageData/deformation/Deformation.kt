package spirite.base.imageData.deformation

import com.hackoeur.jglm.support.FastMath
import rb.vectrix.linear.Vec2f
import rb.vectrix.mathUtil.MathUtil
import spirite.base.pen.stroke.DrawPoints
import kotlin.math.min

interface IDeformation {
    fun transform(x: Float, y: Float) : Vec2f
}

class StrokeDeformationPiece(
        val fromX : FloatArray,
        val fromY : FloatArray,
        val toX : FloatArray,
        val toY : FloatArray)
{
    val len by lazy {min(min(fromX.size, fromY.size), min(toX.size, toY.size)) }
}

class StrokeDeformation(
        val pieces : List<StrokeDeformationPiece>) : IDeformation
{
    override fun transform(x: Float, y: Float) : Vec2f {
        val numMutations = pieces.sumBy { it.len }
        val weights = FloatArray(numMutations)

        var offset = 0
        for (piece in pieces) {
            for (i in 0 until piece.len) {
                weights[i + offset] = FastMath.invSqrtFast ((x - piece.fromX[i]).run { this*this } + (y - piece.fromY[i]).run { this*this })
            }

            offset += piece.len
        }

        val sumWeights = weights.sum()

        var newX = x
        var newY = y
        offset = 0
        for (piece in pieces) {
            for (i in 0 until piece.len) {
                newX += (piece.toX[i] - piece.fromX[i]) * weights[i + offset]/sumWeights
                newY += (piece.toY[i] - piece.fromY[i]) * weights[i + offset]/sumWeights
            }
            offset += piece.len
        }
        return Vec2f(newX, newY)
    }
}