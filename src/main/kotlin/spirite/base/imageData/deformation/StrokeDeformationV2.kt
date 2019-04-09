package spirite.base.imageData.deformation

import rb.vectrix.linear.Vec2f
import rb.vectrix.mathUtil.MathUtil
import rb.vectrix.mathUtil.d
import rb.vectrix.mathUtil.f


/***
 * In this itteration of StrokeDeformation, each point can only attatch to the closest weight in a single piece
 */
class StrokeDeformationV2(
        val pieces : List<StrokeDeformationPiece>) : IDeformation
{
    override fun transform(x: Float, y: Float) : Vec2f {
        val numMutations = pieces.sumBy { it.len }

        val weights = pieces.map {piece ->
            (0 until piece.len).asSequence()
                    .map { i -> Pair(i, 1/MathUtil.distance(x.d, y.d, piece.fromX[i].d, piece.fromY[i].d)) }
                    .maxBy { it.second } ?: Pair(0,0.0)
        }

        val sumWeights = weights.sumByDouble { it.second ?: 0.0 }
        if( sumWeights == 0.0) return Vec2f(x,y)


        var newX = x
        var newY = y
        weights.forEachIndexed { pieceIndex, (pieceSubIndex, weight) ->
            if( weight != 0.0) {
                val piece = pieces[pieceIndex]
                newX += ((piece.toX[pieceSubIndex] - piece.fromX[pieceSubIndex]) * weight / sumWeights).f
                newY += ((piece.toY[pieceSubIndex] - piece.fromY[pieceSubIndex]) * weight / sumWeights).f
            }
        }

        return Vec2f(newX, newY)
    }
}