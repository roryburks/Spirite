package spirite.base.imageData.mediums.magLev

import rb.owl.IContract
import rb.vectrix.linear.ITransformF
import rb.vectrix.linear.ImmutableTransformF
import rb.vectrix.linear.Vec2f
import rb.vectrix.linear.Vec3f
import spirite.base.brains.toolset.ColorChangeMode
import spirite.base.brains.toolset.ColorChangeMode.AUTO
import spirite.base.brains.toolset.ColorChangeMode.IGNORE_ALPHA
import spirite.base.imageData.drawer.IImageDrawer
import spirite.base.imageData.drawer.IImageDrawer.*
import spirite.base.imageData.mediums.ArrangedMediumData
import spirite.base.imageData.mediums.BuiltMediumData
import spirite.base.imageData.mediums.CompositeSource
import spirite.base.imageData.undo.ImageAction
import spirite.base.pen.PenState
import spirite.base.pen.stroke.StrokeBuilder
import spirite.base.pen.stroke.StrokeParams
import spirite.base.pen.stroke.StrokeParams.Method
import spirite.base.util.Color
import spirite.base.util.linear.Rect
import spirite.base.util.linear.RectangleUtil

class MaglevImageDrawer(
        arranged: ArrangedMediumData,
        maglev: MaglevMedium)
    :IImageDrawer,
        IStrokeModule by MaglevStrokeModule(arranged),
        ITransformModule by MaglevTransformModule(arranged, maglev),
        IColorChangeModule by MaglevColorChangeModule(arranged, maglev)
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

        strokeBuilder.end()
        workspace.compositor.compositeSource = null
    }
}

class MaglevTransformModule(
        val arranged: ArrangedMediumData,
        val maglev: MaglevMedium)
    : ITransformModule
{
    val workspace get() = arranged.handle.workspace

    // Note: Code mostly duplicated from DefaultImageDrawer

    override fun transform(trans: ITransformF) {
        val rect = arranged.handle.run { Rect(x,y,width, height) }

        val cx = rect.x + rect.width /2f
        val cy = rect.y + rect.height /2f

        val effectiveTrans = ImmutableTransformF.Translation(cx,cy) * trans * ImmutableTransformF.Translation(-cx,-cy)

        val det = effectiveTrans.determinantF * 0.8f + 0.2f

        maglev.applyTrasformation(arranged, "Transform Maglev Layer") {
            val vec2 = effectiveTrans.apply(Vec2f(it.xf, it.yf))
            Vec3f(vec2.xf, vec2.yf, it.zf * det)
        }
    }

    override fun startManipulatingTransform(): Rect? {
        val tool = workspace.toolset.Reshape

        workspace.compositor.compositeSource = CompositeSource(arranged, false) {gc ->
            val medium = arranged.handle.medium
            val cx = medium.width / 2f + medium.x
            val cy = medium.height / 2f + medium.y

            val effectiveTrans = ImmutableTransformF.Translation(cx,cy) * tool.transform * ImmutableTransformF.Translation(-cx,-cy)

            gc.transform = effectiveTrans
            arranged.handle.medium.render(gc)
        }

        val m = arranged.handle
        return RectangleUtil.circumscribeTrans(Rect(m.x, m.y, m.width, m.height), arranged.tMediumToWorkspace)
    }

    override fun stepManipulatingTransform() {
        workspace.compositor.triggerCompositeChanged()
    }

    override fun endManipulatingTransform() {
        workspace.compositor.compositeSource = null
    }

}

class MaglevColorChangeModule(val arranged: ArrangedMediumData, val maglev: MaglevMedium) : IColorChangeModule
{
    override fun changeColor(from: Color, to: Color, mode: ColorChangeMode) {
        val things = maglev.things
        things.forEach {
            if( it is MaglevStroke) {
                val color = it.params.color
                if( mode == AUTO || (color.red == from.red && color.blue == from.blue && color.green == from.green &&
                        (color.alpha == from.alpha || mode == IGNORE_ALPHA)))
                {
                    it.params = it.params.copy(color = to)
                }
            }
        }

        arranged.handle.workspace.undoEngine.performAndStore(object : ImageAction(arranged) {
            override val description: String get() = "Color Change Maglev Layer"
            override val isHeavy: Boolean get() = true

            override fun performImageAction(built: BuiltMediumData) {
                built.rawAccessComposite {it.graphics.clear()}
                things.forEach { it.draw(built) }
            }
        })
    }
}