package spirite.base.image_data.mediums.maglev

import spirite.base.graphics.DynamicImage
import spirite.base.graphics.IImage
import spirite.base.image_data.ImageWorkspace
import spirite.base.image_data.ImageWorkspace.BuildingMediumData
import spirite.base.image_data.layers.puppet.BasePuppet.BaseBone
import spirite.base.image_data.mediums.BuiltMediumData
import spirite.base.image_data.mediums.DoerOnGC
import spirite.base.image_data.mediums.DoerOnRaw
import spirite.base.image_data.mediums.IMedium
import spirite.base.image_data.mediums.drawer.IImageDrawer
import spirite.base.image_data.mediums.maglev.parts.MagLevFill
import spirite.base.image_data.mediums.maglev.parts.MagLevStroke
import spirite.base.image_data.selection.SelectionMask
import spirite.base.pen.PenTraits.PenState
import spirite.base.util.interpolation.Interpolator2D
import spirite.base.util.linear.MatTrans
import spirite.hybrid.HybridHelper
import java.util.*

/**
 * A Maglev Internal Image is an image that floats just above the surface,
 * not quite planting its feat in the ground.  Essentially it is a kind of
 * Scalable Vector Image, storing all the different stroke and fill actions
 * as logical vertex data rather than rendered pixel data, allowing them to
 * be scaled/rotated without generation loss	.
 */
class MaglevMedium  (
        val context: ImageWorkspace,
        things: List<AMagLevThing>? = null
): IMedium
{
    init {
        things?.forEach {
            if( it.id == -1)
                it.id = workingId++
            workingId = Math.max(workingId, it.id+1)
        }
    }

    //internal
    var things = things?.toMutableList() ?: mutableListOf()

    //internal
    var builtImage: DynamicImage? = null
    private var isBuilt = false

    private var workingId = 0
    var building = false; private set

    private constructor(other: MaglevMedium) : this( other.context, null) {
        this.things = MutableList( other.things.size, { other.things.get(it).clone()})

        this.isBuilt = other.isBuilt
        this.builtImage = if (other.builtImage == null) null else other.builtImage!!.deepCopy()
        this.workingId = other.workingId
    }

    //internal
    fun addThing(thing: AMagLevThing, back: Boolean) {
        if (thing.id == -1)
            thing.id = workingId++
        workingId = Math.max(workingId, thing.id + 1)
        if (back)
            things.add(0, thing)
        else
            things.add(thing)
    }

    //internal
    fun removeThing(toRemove: AMagLevThing) {
        val id = toRemove.id

        val it = things.iterator()
        while (it.hasNext()) {
            val thing = it.next()
            if (thing is MagLevFill) {
                val segments = thing.segments
                segments.removeIf { s -> s.strokeId == id }

                if (segments.size == 0)
                    it.remove()
            }
        }
        things.remove(toRemove)

        unbuild()
    }

    override val width: Int get() {
        Build()
        return if (isBuilt) builtImage!!.width else context.width
    }

    override val height: Int get() {
        Build()
        return if (isBuilt) builtImage!!.height else context.height
    }

    override val dynamicX: Int get() {
        Build()
        return builtImage!!.xOffset
    }
    override val dynamicY: Int get() {
        Build()
        return if (isBuilt) builtImage!!.yOffset else 0
    }
    override val type = IMedium.InternalImageTypes.MAGLEV

    override fun build(building: BuildingMediumData): BuiltMediumData {
        return MaglevBuiltData(building)
    }

    override fun getImageDrawer(building: BuildingMediumData): IImageDrawer {
        return MaglevImageDrawer(this, building)
    }

    override fun dupe(): IMedium {
        return MaglevMedium(this)
    }

    override fun copyForSaving(): IMedium {
        return MaglevMedium(this)
    }


    override fun readOnlyAccess(): IImage {
        Build()
        return builtImage!!.base
    }

    override fun flush() {
        builtImage?.flush()
        builtImage = null
        isBuilt = false
    }


    // ==== Hard Junk
    //internal
    fun splitStroke(strokeId: Int, points: FloatArray) {
        val index = things.indices.first { things[it].id == strokeId }
        val stroke = things[index] as MagLevStroke
        val direct = stroke.direct

        things.removeAt(index)


        // Step 1: Split the Stroke
        val addedStrokes = arrayOfNulls<MagLevStroke>(points.size / 2 + 1)
        var inStroke = true
        var start = 0f
        var i = 0
        while (i < points.size || inStroke) {
            if (inStroke) {
                if (i == points.size && start != (stroke.states.size - 1).toFloat() || i < points.size && points[i] != start) {
                    val states = ArrayList<PenState>()
                    if (start != Math.round(start).toFloat()) {
                        val t = Math.floor(direct.getNearIndex(start).toDouble()).toInt()

                        states.add(PenState(direct.x[t], direct.y[t], direct.w[t]))
                    }

                    if (points.size > i) {
                        val endIndex = points[i]

                        var c = Math.ceil(start.toDouble()).toInt()
                        while (c < endIndex) {
                            states.add(stroke.states[c])
                            ++c
                        }

                        if (endIndex != Math.round(endIndex).toFloat()) {
                            val t = Math.ceil(direct.getNearIndex(endIndex).toDouble()).toInt()

                            states.add(PenState(direct.x[t], direct.y[t], direct.w[t]))
                        }
                    } else {
                        for (c in Math.ceil(start.toDouble()).toInt() until stroke.states.size)
                            states.add(stroke.states[c])
                    }

                    val newStroke = MagLevStroke(states.toTypedArray(), stroke.params)
                    (newStroke as AMagLevThing).id = workingId++
                    things.add(index, newStroke)

                    addedStrokes[i / 2] = newStroke
                }
            } else {
                start = points[i]
            }
            inStroke = !inStroke
            ++i
        }

        // Step 2: Re-map the Fills (todo)
        for (thing in things) {
            (thing as? MagLevFill)?.segments?.removeIf { ss -> ss.strokeId == stroke.id }
        }
    }

    //internal
    fun Build() {
        if (!building) {
            building = true
            if (!isBuilt) {
                val dyn = DynamicImage(context, HybridHelper.createNillImage(), 0, 0)

                dyn.doOnGC({ gc ->
                    val built = this.build(BuildingMediumData(context.getHandleFor(this), 0, 0))
                    val mask: SelectionMask? = null
                    for (thing in things)
                        thing.draw(built, mask, gc, this)
                }, MatTrans())
                builtImage = dyn
            }
            isBuilt = true
            building = false
        }
    }

    //internal
    fun unbuild() {
        if (this.isBuilt) {
            this.isBuilt = false
            builtImage!!.flush()
            builtImage = null
        }
    }

    fun contortBones(bone: BaseBone, state: Interpolator2D) {
        this.things = contortBones(this.things, bone, state)
        unbuild()
        Build()
        //context.getHandleFor(this).refresh();
    }

    /** Be careful with these things, they can break.  */
    fun getThings2(): List<AMagLevThing> {
        return ArrayList(things)
    }


    inner class MaglevBuiltData(building: BuildingMediumData) : BuiltMediumData(building) {
        init {
            Build()
        }

        override val _sourceToComposite: MatTrans get() = MatTrans()
        override val _screenToSource: MatTrans by lazy {
            val strans = MatTrans(trans)
            strans.preTranslate(builtImage!!.xOffset.toFloat(), builtImage!!.yOffset.toFloat())
            strans
        }

        override val compositeWidth: Int get() = context.width
        override val compositeHeight: Int get() = context.height

        override val sourceWidth: Int = builtImage!!.width
        override val sourceHeight: Int = builtImage!!.height

        override fun _doOnGC(doer: DoerOnGC) {
            builtImage!!.doOnGC(doer, _screenToSource)
        }

        override fun _doOnRaw(doer: DoerOnRaw) {
            builtImage!!.doOnRaw(doer, _screenToSource)
        }
        /*
        internal var trans: MatTrans = building.trans

        override fun getWidth(): Int {
            return context.width
        }

        override fun getHeight(): Int {
            return context.height
        }

        override fun convert(p: Vec2): Vec2 {
            return p
        }

        override fun getBounds(): Rect {
            // TODO
            return Rect(trans.m02.toInt(), trans.m12.toInt(), context.width, context.height)
        }

        override fun draw(gc: GraphicsContext) {
            val oldTrans = gc.transform
            gc.preTransform(trans)
            gc.drawImage(builtImage!!.base, builtImage!!.xOffset, builtImage!!.yOffset)
            gc.transform = oldTrans
        }

        override fun drawBorder(gc: GraphicsContext) {
            val oldTrans = gc.transform
            gc.preTransform(trans)
            gc.drawRect(0, 0, context.width, context.height)
            gc.transform = oldTrans
        }

        override fun getCompositeTransform(): MatTrans {
            return MatTrans(trans)
        }

        override fun getScreenToImageTransform(): MatTrans {
            try {
                return trans.createInverse()
            } catch (e: NoninvertableException) {
                return MatTrans()
            }

        }

        // Counter-intuitively, checking in and checking out of a MaglevInternalImage
        //	can be a thing that makes sense to do as the StrokeEngine uses it for the
        //	behavior we want.
        override fun _doOnGC(doer: DoerOnGC) {
            builtImage!!.doOnGC(doer, trans)
        }

        override fun _doOnRaw(doer: DoerOnRaw) {
            builtImage!!.doOnRaw(doer, trans)
        }*/
    }


    fun getThingById(Id: Int): AMagLevThing? {
        for (thing in things) {
            if (thing.id == Id)
                return thing
        }
        return null
    }
}
