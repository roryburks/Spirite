package spirite.base.imageData.mediums.drawer

import spirite.base.imageData.IImageWorkspace
import spirite.base.imageData.mediums.ArrangedMediumData
import spirite.base.imageData.mediums.CompositeSource
import spirite.base.imageData.mediums.drawer.IImageDrawer.IStrokeModule
import spirite.base.pen.PenState
import spirite.base.pen.stroke.StrokeBuilder
import spirite.base.pen.stroke.StrokeParams
import spirite.base.pen.stroke.StrokeParams.Method
import spirite.base.util.interpolation.CubicSplineInterpolator2D
import spirite.base.util.interpolation.CubicSplineInterpolatorND

class DefaultImageDrawer(
        val arranged: ArrangedMediumData)
    :IImageDrawer,
        IStrokeModule
{
    val workspace : IImageWorkspace get() = arranged.handle.workspace


    // region IStrokeModule
    private var strokeBuilder : StrokeBuilder? = null

    override fun canDoStroke(method: Method) = true

    override fun startStroke(params: StrokeParams, ps: PenState): Boolean {
        val strokeDrawer = workspace.strokeProvider.getStrokeDrawer(params)
        strokeBuilder = StrokeBuilder(strokeDrawer, params, arranged, null, CubicSplineInterpolator2D())

        workspace.compositor.compositeSource = CompositeSource(
                arranged,
                {

                }
        )
        return strokeBuilder!!.start(ps)
    }

    override fun stepStroke(ps: PenState) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun endStroke() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
    // endregion

}