package spirite.base.pen.stroke

import rb.vectrix.linear.ITransformF
import rb.vectrix.linear.Vec2f
import spirite.base.imageData.mediums.ArrangedMediumData
import spirite.base.pen.PenState
import spirite.base.pen.stroke.StrokeParams.InterpolationMethod.CUBIC_SPLINE
import spirite.base.pen.stroke.StrokeParams.InterpolationMethod.NONE
import spirite.base.util.interpolation.CubicSplineInterpolator2D
import spirite.base.util.interpolation.Interpolator2D

class StrokeBuilder(
        val strokeDrawer: IStrokeDrawer,
        val params: StrokeParams,
        val arranged: ArrangedMediumData
) {
    private val tWorkspaceToComposite : ITransformF
    private val baseStates = mutableListOf<PenState>()
    private val width: Int
    private val height: Int

    val interpolator : Interpolator2D? = when( params.interpolationMethod) {
        NONE -> null
        CUBIC_SPLINE -> CubicSplineInterpolator2D()
    }

    init {
        val built = arranged.built
        tWorkspaceToComposite = built.tWorkspaceToComposite
        width = built.width
        height = built.height
    }

    val currentPoints : DrawPoints get() {
        val current = _currentPoints ?: DrawPointsBuilder.buildPoints(interpolator, baseStates, params.dynamics)
        _currentPoints = current
        return current
    }
    private var _currentPoints : DrawPoints? = null

    fun start( ps: PenState) : Boolean{

        val cps = convertPS(ps)
        baseStates.add(cps)
        interpolator?.addPoint(cps.x, cps.y)
        _currentPoints = null

        return strokeDrawer.start(this, width, height)
    }

    fun step( ps: PenState) : Boolean{
        val cps = convertPS(ps)

        if( cps.x != baseStates.lastOrNull()?.x || cps.y != baseStates.lastOrNull()?.y) {
            baseStates.add(cps)
            interpolator?.addPoint(cps.x, cps.y)
            _currentPoints = null
            return strokeDrawer.step()
        }
        return false
    }

    fun end() {
        strokeDrawer.end()
    }

    private fun convertPS( ps : PenState) : PenState {
        val transformed = tWorkspaceToComposite.apply(Vec2f(ps.x, ps.y))
        val pressure = params.dynamics.getSize(ps)
        return PenState( transformed.xf, transformed.yf, pressure)
    }
}

