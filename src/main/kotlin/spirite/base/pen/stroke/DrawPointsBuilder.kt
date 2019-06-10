package spirite.base.pen.stroke

import rb.jvm.vectrix.compaction.FloatCompactor
import rb.jvm.vectrix.interpolation.Interpolator2D
import rb.vectrix.mathUtil.MathUtil
import spirite.base.pen.PenState

object DrawPointsBuilder {

    // The Interpolator tick distance.  Lower means smoother but more rendering time (especially with Maglev layers)
    private const val DIFF = 1.0f

    fun buildPoints(interpolator: Interpolator2D?, penStates: List<PenState>, dynamics: PenDynamics?) : DrawPoints {
        val num = penStates.size

        return when {
            interpolator == null -> {
                val x_ = penStates.map { it.x }.toFloatArray()
                val y_ = penStates.map { it.y }.toFloatArray()
                val w_ = penStates.map { dynamics?.getSize(it) ?: it.pressure }.toFloatArray()
                DrawPoints(x_, y_, w_)
            }
            num == 0 -> DrawPoints(FloatArray(0), FloatArray(0), FloatArray(0))
            num == 1 -> DrawPoints(floatArrayOf(penStates.first().x), floatArrayOf(penStates.first().y), floatArrayOf(penStates.first().pressure))
            else -> {
                val fcX = FloatCompactor()
                val fcY = FloatCompactor()
                val fcW = FloatCompactor()

                var interpos = 0f

                fun addPoint() {
                    val ip = interpolator.evalExt(interpos)
                    val state = PenState(ip.x, ip.y, MathUtil.lerp(penStates[ip.left].pressure, penStates[ip.right].pressure, ip.lerp))
                    fcX.add(ip.x)
                    fcY.add(ip.y)
                    fcW.add(dynamics?.getSize(state) ?: state.pressure)
                }
                addPoint()

                while( interpos + DIFF < interpolator.curveLength) {
                    interpos += DIFF
                    addPoint()
                }

                DrawPoints(fcX.toArray(), fcY.toArray(), fcW.toArray())
            }
        }
    }

    fun buildIndexedPoints(interpolator: Interpolator2D?, penStates: List<PenState>, dynamics: PenDynamics?): IndexedDrawPoints {
        val num = penStates.size

        return when {
            interpolator == null -> {
                val x_ = penStates.map { it.x }.toFloatArray()
                val y_ = penStates.map { it.y }.toFloatArray()
                val w_ = penStates.map { dynamics?.getSize(it) ?: it.pressure }.toFloatArray()
                val t_ = penStates.mapIndexed { it, _ -> it.toFloat()}.toFloatArray()
                IndexedDrawPoints(x_, y_, w_, t_)
            }
            num == 0 -> IndexedDrawPoints(FloatArray(0), FloatArray(0), FloatArray(0), FloatArray(0))
            num == 1 -> IndexedDrawPoints(floatArrayOf(penStates.first().x), floatArrayOf(penStates.first().y), floatArrayOf(penStates.first().pressure), floatArrayOf(0f))
            else -> {
                val fcX = FloatCompactor()
                val fcY = FloatCompactor()
                val fcW = FloatCompactor()
                val fcT = FloatCompactor()

                var interpos = 0f

                fun addPoint() {
                    val ip = interpolator.evalExt(interpos)
                    val state = PenState(ip.x, ip.y, MathUtil.lerp(penStates[ip.left].pressure, penStates[ip.right].pressure, ip.lerp))
                    fcX.add(ip.x)
                    fcY.add(ip.y)
                    fcW.add(dynamics?.getSize(state) ?: state.pressure)
                    fcT.add(ip.left + ip.lerp)
                }
                addPoint()

                while( interpos + DIFF < interpolator.curveLength) {
                    interpos += DIFF
                    addPoint()
                }

                IndexedDrawPoints(fcX.toArray(), fcY.toArray(), fcW.toArray(), fcT.toArray())
            }
        }

    }
}