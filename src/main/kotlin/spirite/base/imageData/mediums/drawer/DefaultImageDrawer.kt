package spirite.base.imageData.mediums.drawer

import spirite.base.imageData.IImageWorkspace
import spirite.base.imageData.mediums.ArrangedMediumData
import spirite.base.imageData.mediums.BuiltMediumData
import spirite.base.imageData.mediums.CompositeSource
import spirite.base.imageData.mediums.drawer.IImageDrawer.IStrokeModule
import spirite.base.imageData.undo.ImageAction
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
        strokeBuilder = StrokeBuilder(strokeDrawer, params, arranged)

        workspace.compositor.compositeSource = CompositeSource(
                arranged,
                {strokeDrawer.draw(it)})

        if(strokeBuilder!!.start(ps))
            arranged.handle.refresh()

        return true
    }

    override fun stepStroke(ps: PenState) {
        if(strokeBuilder?.step(ps) == true)
            arranged.handle.refresh()
    }

    override fun endStroke() {
        val sb = strokeBuilder ?: return

        // NOTE: Storing the bakedDrawPoints rather than the baseStates, this means two things:
        //  1: Far more points are stored than is necessary, but don't need to be recalculated every time (both minimal)
        //  2: You need to use rawAccessComposite rather than drawToComposite as the points are already transformed
        val bakedDrawPoints = sb.currentPoints
        val params = sb.params
        workspace.undoEngine.performAndStore( object : ImageAction(arranged) {
            override val description: String get() = "Pen Stroke"

            override fun performImageAction(built: BuiltMediumData) {
                val drawer = built.arranged.handle.workspace.strokeProvider.getStrokeDrawer(params)
                built.rawAccessComposite {
                    drawer.batchDraw(it.graphics, bakedDrawPoints, params, built.width, built.height)
                }
            }
        })

        sb.end()
        workspace.compositor.compositeSource = null
    }
    // endregion

}