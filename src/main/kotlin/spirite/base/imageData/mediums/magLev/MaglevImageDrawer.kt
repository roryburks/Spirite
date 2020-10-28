package spirite.base.imageData.mediums.magLev

import rb.glow.Color
import rbJvm.glow.SColor
import rb.hydra.anyTiamatGrindSync
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
import spirite.base.imageData.deformation.IDeformation
import spirite.base.imageData.drawer.IImageDrawer
import spirite.base.imageData.drawer.IImageDrawer.*
import spirite.base.imageData.mediums.ArrangedMediumData
import spirite.base.imageData.mediums.BuiltMediumData
import spirite.base.imageData.mediums.HandleCompositeSource
import spirite.base.imageData.mediums.magLev.MaglevFill.StrokeSegment
import spirite.base.imageData.mediums.magLev.selecting.MaglevAnchorLiftModule
import spirite.base.imageData.mediums.magLev.selecting.MaglevLiftSelectionModule
import spirite.base.pen.PenState
import spirite.base.pen.stroke.DrawPoints
import spirite.base.pen.stroke.StrokeBuilder
import spirite.base.pen.stroke.StrokeParams
import spirite.base.pen.stroke.StrokeParams.Method
import spirite.base.util.linear.Rect
import spirite.base.util.linear.RectangleUtil
import kotlin.math.abs

class MaglevImageDrawer(
        arranged: ArrangedMediumData,
        maglev: MaglevMedium)
    :IImageDrawer,
        IClearModule by MaglevClearModule(arranged),
        IStrokeModule by MaglevStrokeModule(arranged),
        ITransformModule by MaglevTransformModule(arranged),
        IColorChangeModule by MaglevColorChangeModule(arranged),
        IMagneticFillModule by MaglevMagneticFillModule(arranged, maglev),
        IDeformDrawer by MaglevDeformationModule(arranged, maglev),
        IMagneticEraseModule by MaglevMagneticEraseModule(arranged, maglev),
        ILiftSelectionModule by MaglevLiftSelectionModule(arranged),
        IAnchorLiftModule by MaglevAnchorLiftModule(arranged, maglev)

class MaglevClearModule( val arranged: ArrangedMediumData) : IClearModule {
    override fun clear() {
        arranged.handle.workspace.undoEngine.performAndStoreMaglevImageAction(arranged, "Clear Maglev Layer"){ built, maglev->
            maglev.thingsMap.clear()
            built.rawAccessComposite { it.graphics.clear() }
        }
    }
}

class MaglevStrokeModule(val arranged: ArrangedMediumData) : IStrokeModule {
    val workspace get() = arranged.handle.workspace
    lateinit var strokeBuilder : StrokeBuilder

    override fun canDoStroke(method: Method) = true

    override fun startStroke(params: StrokeParams, ps: PenState): Boolean {
        val strokeDrawer = workspace.strokeProvider.getStrokeDrawer(params)
        strokeBuilder = StrokeBuilder( strokeDrawer, params, arranged)

        workspace.compositor.compositeSource = HandleCompositeSource(arranged) {strokeDrawer.draw(it)}

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

class MaglevTransformModule(val arranged: ArrangedMediumData)
    : ITransformModule
{
    val workspace get() = arranged.handle.workspace

    // Note: Code mostly duplicated from DefaultImageDrawer

    override fun transform(trans: ITransformF, centered : Boolean) {
        val rect = arranged.handle.run { Rect(x,y,width, height) }

        val cx = rect.x + rect.width /2f
        val cy = rect.y + rect.height /2f

        val effectiveTrans =
                if( centered) ImmutableTransformF.Translation(cx,cy) * trans * ImmutableTransformF.Translation(-cx,-cy)
                else trans

        val det = effectiveTrans.determinantF * 0.8f + 0.2f


        arranged.handle.workspace.undoEngine.performAndStore(object : MaglevImageAction(arranged) {

            override val description: String get() = "Transform Maglev Layer"
            override val isHeavy: Boolean get() = true

            override fun performMaglevAction(built: BuiltMediumData, maglev: MaglevMedium) {
                val things = maglev.thingsMap.values
                things.asSequence()
                        .filterIsInstance<IMaglevPointwiseThing>()
                        .forEach { thing -> thing.transformPoints {
                                val vec2 = effectiveTrans.apply(Vec2f(it.xf, it.yf))
                                Vec3f(vec2.xf, vec2.yf, it.zf * det)
                            }
                        }
                built.rawAccessComposite {it.graphics.clear()}
                things.forEach { it.draw(built) }
            }
        })
    }

    override fun startManipulatingTransform(): Rect? {
        val tool = workspace.toolset.Reshape

        workspace.compositor.compositeSource = HandleCompositeSource(arranged, false) { gc ->
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

class MaglevColorChangeModule(val arranged: ArrangedMediumData) : IColorChangeModule
{
    override fun changeColor(from: Color, to: Color, mode: ColorChangeMode) {
        arranged.handle.workspace.undoEngine.performAndStore(object : MaglevImageAction(arranged) {
            override val description: String get() = "Color Change Maglev Layer"
            override val isHeavy: Boolean get() = true
            override fun performMaglevAction(built: BuiltMediumData, maglev: MaglevMedium) {
                val things = maglev.thingsMap.values
                things.asSequence()
                        .filterIsInstance<IMaglevColorwiseThing>()
                        .forEach { it.transformColor { color ->
                            if( mode == AUTO || (color.red == from.red && color.blue == from.blue && color.green == from.green &&
                                            (color.alpha == from.alpha || mode == IGNORE_ALPHA)))
                                to
                            else
                                color
                        } }
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

    var ss : BuildingStrokeSegment? =  null
    val segments by lazy { mutableListOf<BuildingStrokeSegment>()}


    // region MagFill Coords
    private fun resetMagFills() {
        _magFillXs = null
        _magFillYs = null
    }

    private fun recalcMagFills() : Pair<FloatArray,FloatArray> {
        val totalLen = segments.sumBy { abs(it.travel) + 1 }
        val outX = FloatArray(totalLen)
        val outY = FloatArray(totalLen)
        var i = 0
        segments.forEach { seg ->
            val stroke = maglev.thingsMap[seg.strokeId] as MaglevStroke
            val sign = if(seg.travel < 0) -1 else 1
            (0..abs(seg.travel)).forEach { c ->
                outX[i] = stroke.drawPoints.x[seg.pivotPoint + c * sign]
                outY[i] = stroke.drawPoints.y[seg.pivotPoint + c * sign]
                ++i
            }
        }

        _magFillXs = outX
        _magFillYs = outY
        return Pair(outX,outY)
    }
    private var _magFillXs: FloatArray? = null
    private var _magFillYs: FloatArray? = null
    override val magFillXs get() = _magFillXs ?: recalcMagFills().first
    override val magFillYs get() = _magFillYs ?: recalcMagFills().second
    // endregion

    override fun startMagneticFill() {}

    override fun endMagneticFill(color: SColor, mode: MagneticFillMode) {
        if( segments.filter{maglev.thingsMap[it.strokeId] !is MaglevStroke}.any()){
            println("brk")
        }
        val fill = MaglevFill(segments.map {StrokeSegment(it.strokeId, it.pivotPoint, it.pivotPoint + it.travel)}, mode, color)
        maglev.addThing(fill,  arranged, "Magnetic Fill")
    }

    override fun anchorPoints(x: Float, y: Float, r: Float, locked: Boolean, relooping: Boolean) {
        val ss = ss
        val (closestDist, closest) = findClosestStroke( x, y) ?: return
        if( closestDist > r) return

        if( closest.strokeId == ss?.strokeId) {
            val stroke = maglev.thingsMap[closest.strokeId] as MaglevStroke
            val sx = stroke.drawPoints.x[closest.pivotPoint]
            val sy = stroke.drawPoints.y[closest.pivotPoint]

            if( !locked && (Math.abs(ss.travel - (closest.pivotPoint - ss.pivotPoint)) > 1.5 * MathUtil.distance(x,y,sx,sy)))
            {
                this.ss = closest
                segments.add(ss)
            }
            else
                ss.travel = closest.pivotPoint - ss.pivotPoint
            resetMagFills()
        }
        else if(!locked || ss == null){
            // Can reach here either because we are not currently latched onto something or because
            //  (a) not currently latched onto something
            //  (b) closer to something else
            this.ss = closest
            segments.add(closest)
            resetMagFills()
        }
    }

    override fun erasePoints(x: Float, y: Float, r: Float) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private data class StrokePointContext(val strokeIndex: Int, val index: Int, val drawPoints: DrawPoints)
    fun findClosestStroke( x: Float, y: Float) : Pair<Double,BuildingStrokeSegment>?
    {
        val (dist, closest) = maglev.thingsMap.asSequence()
                .mapNotNull { entry ->
                    val iMaglevThing = entry.value
                    if( iMaglevThing is MaglevStroke) Pair(entry.key, iMaglevThing.drawPoints)
                    else null }
                .flatMap { (index, drawPoints) ->
                    (0 until drawPoints.length).asSequence().map { StrokePointContext(index, it, drawPoints) } }
                .miniTiamatGrindSync { MathUtil.distance(x, y, it.drawPoints.x[it.index], it.drawPoints.y[it.index]).d } ?: return null

        return Pair(dist, BuildingStrokeSegment(closest.strokeIndex, closest.index))
    }
}

class MaglevDeformationModule(val arranged: ArrangedMediumData, val maglev: MaglevMedium) : IDeformDrawer {
    override fun deform(deformation: IDeformation) {


        arranged.handle.workspace.undoEngine.performAndStore(object : MaglevImageAction(arranged) {
            override val description: String get() = "Transform Maglev Layer"
            override val isHeavy: Boolean get() = true

            override fun performMaglevAction(built: BuiltMediumData, maglev: MaglevMedium) {
                val things = maglev.thingsMap.values
                things.asSequence()
                        .filterIsInstance<IMaglevPointwiseThing>()
                        .forEach { it.transformPoints{
                            val vec2 = deformation.transform(it.xf, it.yf)
                            Vec3f(vec2.xf, vec2.yf, it.zf)
                        } }
                built.rawAccessComposite {it.graphics.clear()}
                things.forEach { it.draw(built) }
            }
        })

    }

}

class MaglevMagneticEraseModule( val arranged: ArrangedMediumData, val maglev: MaglevMedium) : IMagneticEraseModule
{
    override fun erase(x: Float, y: Float) {
        val things = maglev.thingsMap
        val removedThings = HashSet<IMaglevThing>()

        val strokesToRemove = things.values
                .filterIsInstance<MaglevStroke>()
                .filter {stroke ->
                    val strokeWidth = stroke.params.width
                    stroke.drawPoints.run {
                        (0 until length).asSequence()
                                .anyTiamatGrindSync { MathUtil.distance(this.x[it], this.y[it], x, y) < this.w[it] * strokeWidth }}}

        removedThings.addAll(strokesToRemove)

        val removedIds = (0 until things.size)
                .filter { removedThings.contains(things[it]) }
                .toSet()


        val fillsToRemove = maglev.thingsMap.values
                .filterIsInstance<MaglevFill>()
                .filter { fill -> fill.segments.any{removedIds.contains(it.strokeId)} }

        // Could also do Polygon contains logic

        removedThings.addAll(fillsToRemove)

        if( removedThings.any())
            maglev.removeThings(removedThings, arranged, "Maglev Erase Action")
    }
}