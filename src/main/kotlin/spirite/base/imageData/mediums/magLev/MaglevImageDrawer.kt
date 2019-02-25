package spirite.base.imageData.mediums.magLev

import spirite.base.imageData.drawer.IImageDrawer
import spirite.base.imageData.drawer.IImageDrawer.IStrokeModule
import spirite.base.imageData.mediums.ArrangedMediumData
import spirite.base.imageData.mediums.CompositeSource
import spirite.base.pen.PenState
import spirite.base.pen.stroke.StrokeBuilder
import spirite.base.pen.stroke.StrokeParams
import spirite.base.pen.stroke.StrokeParams.Method

class MaglevImageDrawer( arranged: ArrangedMediumData)
    :IImageDrawer,
        IStrokeModule by MaglevStrokeModule(arranged)
{
}

class MaglevStrokeModule(val arranged: ArrangedMediumData) : IStrokeModule {
    val workspace get() = arranged.handle.workspace
    lateinit var strokeBuilder : StrokeBuilder

    override fun canDoStroke(method: Method) = true

    override fun startStroke(params: StrokeParams, ps: PenState): Boolean {
        val strokeDrawer = workspace.strokeProvider.getStrokeDrawer(params)
        strokeBuilder = StrokeBuilder( strokeDrawer, params, arranged)

        workspace.compositor.compositeSource = CompositeSource(arranged) {strokeDrawer.draw(it)}

        if( strokeBuilder.start(ps))
            arranged.handle.refresh()

        return true
    }

    override fun stepStroke(ps: PenState) {
        if( strokeBuilder.step(ps))
            workspace.compositor.triggerCompositeChanged()
    }

    override fun endStroke() {
        // NOTE: Storing the bakedDrawPoints rather than the baseStates, this means two things:
        //  1: Far more drawPoints are stored than is necessary, but don't need to be recalculated every time (both minimal)
        //  2: You need to use rawAccessComposite rather than drawToComposite as the drawPoints are already transformed
        val bakedDrawPoints = strokeBuilder.currentPoints
        val params = strokeBuilder.params
        val maglevLayer = arranged.handle.medium as MaglevMedium

        maglevLayer.addThing(MaglevStroke(params, bakedDrawPoints), arranged, "Pen Stroke on Maglev Layer")
    }

}
