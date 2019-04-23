package spirite.base.imageData.deformation

import rb.vectrix.linear.Vec2f
import rb.vectrix.mathUtil.MathUtil
import rb.vectrix.mathUtil.d
import rb.vectrix.mathUtil.f

class StrokeDeformation(
        val pieces : List<StrokeDeformationPiece>) : IDeformation
{
    override fun transform(x: Float, y: Float) : Vec2f {
        val numMutations = pieces.sumBy { it.len }
        val weights = DoubleArray(numMutations)

        var offset = 0
        for (piece in pieces) {
            for (i in 0 until piece.len) {
                weights[i + offset] = 1/ MathUtil.distance(x.d, y.d, piece.fromX[i].d, piece.fromY[i].d)
            }

            offset += piece.len
        }

        val sumWeights = weights.sum()

        var newX = x
        var newY = y
        offset = 0
        for (piece in pieces) {
            for (i in 0 until piece.len) {
                newX += ((piece.toX[i] - piece.fromX[i]) * weights[i + offset]/sumWeights).f
                newY += ((piece.toY[i] - piece.fromY[i]) * weights[i + offset]/sumWeights).f
            }
            offset += piece.len
        }

        return Vec2f(newX, newY)
    }
}