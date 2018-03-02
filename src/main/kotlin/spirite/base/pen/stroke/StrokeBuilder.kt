package spirite.base.pen.stroke

import spirite.base.imageData.mediums.ArrangedMediumData
import spirite.base.imageData.selection.SelectionMask
import spirite.base.pen.PenState
import spirite.base.util.interpolation.Interpolator2D
import spirite.base.util.linear.Transform
import spirite.base.util.linear.Vec2

class StrokeBuilder(
        val strokeDrawer: IStrokeDrawer,
        val params: StrokeParams,
        val arranged: ArrangedMediumData,
        val dynamics: PenDynamics? = null,
        val interpolator: Interpolator2D? = null
) {
    private val tWorkspaceToComposite : Transform
    private val baseStates = mutableListOf<PenState>()
    private val width: Int
    private val height: Int

    init {
        val built = arranged.built
        tWorkspaceToComposite = built.tWorkspaceToComposite
        width = built.width
        height = built.height
    }

    val currentPoints : DrawPoints get() {
        val current = _currentPoints ?: DrawPointsBuilder.buildPoints(interpolator, baseStates, dynamics)
        _currentPoints = current
        return current
    }
    private var _currentPoints : DrawPoints? = null

    fun start( ps: PenState) : Boolean{
        val cps = convertPS(ps)
        baseStates.add(cps)
        interpolator?.addPoint(cps.x, cps.y)

        return strokeDrawer.start(this, width, height)
    }

    fun step( ps: PenState) : Boolean{
        val cps = convertPS(ps)
        baseStates.add(cps)
        interpolator?.addPoint(cps.x, cps.y)

        return strokeDrawer.step()
    }

    fun end() {
        strokeDrawer.end()
    }

    private fun convertPS( ps : PenState) : PenState {
        val transformed = tWorkspaceToComposite.apply(Vec2(ps.x, ps.y))
        val pressure = dynamics?.getSize(ps) ?: ps.pressure
        return PenState( transformed.x, transformed.y, pressure)
    }
}

