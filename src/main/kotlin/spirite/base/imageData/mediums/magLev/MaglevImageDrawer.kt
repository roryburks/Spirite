package spirite.base.imageData.mediums.magLev

import rb.hydra.miniTiamatGrind
import rb.hydra.miniTiamatGrindSync
import rb.vectrix.linear.ITransformF
import rb.vectrix.linear.ImmutableTransformF
import rb.vectrix.linear.Vec2f
import rb.vectrix.linear.Vec3f
import rb.vectrix.mathUtil.MathUtil
import rb.vectrix.mathUtil.d
import spirite.base.brains.toolset.ColorChangeMode
import spirite.base.brains.toolset.ColorChangeMode.AUTO
import spirite.base.brains.toolset.ColorChangeMode.IGNORE_ALPHA
import spirite.base.brains.toolset.MagneticFillMode
import spirite.base.imageData.drawer.IImageDrawer
import spirite.base.imageData.drawer.IImageDrawer.*
import spirite.base.imageData.mediums.ArrangedMediumData
import spirite.base.imageData.mediums.BuiltMediumData
import spirite.base.imageData.mediums.CompositeSource
import spirite.base.imageData.mediums.magLev.MaglevFill.StrokeSegment
import spirite.base.imageData.undo.ImageAction
import spirite.base.pen.PenState
import spirite.base.pen.stroke.DrawPoints
import spirite.base.pen.stroke.StrokeBuilder
import spirite.base.pen.stroke.StrokeParams
import spirite.base.pen.stroke.StrokeParams.Method
import spirite.base.util.Color
import spirite.base.util.delegates.DerivedLazy
import spirite.base.util.linear.Rect
import spirite.base.util.linear.RectangleUtil
import spirite.pc.gui.SColor

class MaglevImageDrawer(
        arranged: ArrangedMediumData,
        maglev: MaglevMedium)
    :IImageDrawer,
        IStrokeModule by MaglevStrokeModule(arranged),
        ITransformModule by MaglevTransformModule(arranged, maglev),
        IColorChangeModule by MaglevColorChangeModule(arranged, maglev),
        IMagneticFillModule by MaglevMagneticFillModule(arranged, maglev)
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

        val things = maglev.things
        things.asSequence()
                .filterIsInstance<IMaglevPointwiseThing>()
                .forEach { it.transformPoints{
                    val vec2 = effectiveTrans.apply(Vec2f(it.xf, it.yf))
                    Vec3f(vec2.xf, vec2.yf, it.zf * det)
                } }

        arranged.handle.workspace.undoEngine.performAndStore(object : ImageAction(arranged) {
            override val description: String get() = "Transform Maglev Layer"
            override val isHeavy: Boolean get() = true

            override fun performImageAction(built: BuiltMediumData) {
                built.rawAccessComposite {it.graphics.clear()}
                things.forEach { it.draw(built) }
            }
        })
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
        things.asSequence()
                .filterIsInstance<IMaglevColorwiseThing>()
                .forEach { it.transformColor { color ->
                    if( mode == AUTO || (color.red == from.red && color.blue == from.blue && color.green == from.green &&
                                    (color.alpha == from.alpha || mode == IGNORE_ALPHA)))
                        to
                    else
                        color
                } }

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

class MaglevMagneticFillModule(val arranged: ArrangedMediumData, val maglev: MaglevMedium) : IMagneticFillModule {
    data class BuildingStrokeSegment(
            val strokeId: Int,
            val pivotPoint: Int,
            var travel: Int = 0)
    {
    }
    var ss : BuildingStrokeSegment? =  null
    val segments by lazy { mutableListOf<BuildingStrokeSegment>()}

    private val _magFillXs = DerivedLazy<FloatArray>{
        val totalLen = segments.sumBy { it.travel + 1 }
        val out = FloatArray(totalLen)
        var i = 0
        segments.forEach {
            // TODO
        }

        out
    }
    override val magFillXs: FloatArray get()  = TODO()
    override val magFillYs: FloatArray
        get() = TODO("not implemented")

    override fun startMagneticFill() {}

    override fun endMagneticFill(color: SColor, mode: MagneticFillMode) {
        val fill = MaglevFill(segments.map {StrokeSegment(it.strokeId, it.pivotPoint, it.travel)}, color)
        maglev.addThing(fill,  arranged, "Magnetic Fill")
    }

    override fun anchorPoints(x: Float, y: Float, r: Float, locked: Boolean, relooping: Boolean) {
        val ss = ss
        val (closestDist, closest) = findClosestStroke( x, y) ?: return
        if( closestDist > r) return

        if( closest.strokeId == ss?.strokeId) {
            val stroke = maglev.things[maglev.thingMap[closest.strokeId] ?: return] as MaglevStroke
            val sx = stroke.drawPoints.x[closest.pivotPoint]
            val sy = stroke.drawPoints.y[closest.pivotPoint]

            if( !locked && (Math.abs(ss.travel - (closest.pivotPoint - ss.pivotPoint)) > 1.5 * MathUtil.distance(x,y,sx,sy)))
            {
                this.ss = closest
                segments.add(ss)
            }
            else
                ss.travel = closest.pivotPoint - ss.pivotPoint
        }
        else if(!locked || ss == null){
            // Can reach here either because we are not currently latched onto something or because
            //  (a) not currently latched onto something
            //  (b) closer to something else
            this.ss = closest
            segments.add(closest)
        }
    }

    override fun erasePoints(x: Float, y: Float, r: Float) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private data class StrokePointContext(val strokeIndex: Int, val index: Int, val drawPoints: DrawPoints)
    fun findClosestStroke( x: Float, y: Float) : Pair<Double,BuildingStrokeSegment>?
    {
        return null
//        val (dist, closest) = maglev.things.asSequence()
//                .mapIndexedNotNull { index, iMaglevThing ->
//                    if( iMaglevThing is MaglevStroke) Pair(index, iMaglevThing.drawPoints)
//                    else null }
//                .flatMap { (index, drawPoints) ->
//                    (0 until drawPoints.length).asSequence().map { StrokePointContext(index, it, drawPoints) } }
//                .miniTiamatGrindSync { MathUtil.distance(x, y, it.drawPoints.x[it.index], it.drawPoints.y[it.index]).d } ?: return null
//
//        val remappedStrokeId = maglev.thingMap.entries.first { it.value == closest.strokeIndex }.key
//        return Pair(dist, BuildingStrokeSegment(remappedStrokeId, closest.index))
    }
}