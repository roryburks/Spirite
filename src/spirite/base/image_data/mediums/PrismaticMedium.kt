package spirite.base.image_data.mediums

import jdk.nashorn.internal.runtime.options.Option
import spirite.base.graphics.DynamicImage
import spirite.base.graphics.GraphicsContext
import spirite.base.graphics.GraphicsContext.Composite
import spirite.base.graphics.IImage
import spirite.base.graphics.RawImage
import spirite.base.image_data.ImageWorkspace
import spirite.base.image_data.ImageWorkspace.BuildingMediumData
import spirite.base.image_data.mediums.drawer.DefaultImageDrawer
import spirite.base.image_data.mediums.drawer.IImageDrawer
import spirite.base.util.MUtil
import spirite.base.util.linear.MatTrans
import spirite.base.util.linear.MatTrans.NoninvertableException
import spirite.base.util.linear.Rect
import spirite.base.util.linear.Vec2
import spirite.hybrid.HybridHelper
import spirite.hybrid.HybridUtil
import spirite.hybrid.HybridUtil.UnsupportedImageTypeException
import spirite.hybrid.MDebug
import spirite.hybrid.MDebug.ErrorType

import java.util.ArrayList
import java.util.Optional

/***
 * PrismaticInternalImages are a type of Internal Image that behave similarly to
 * Dynamic images, but separate each layer by color.
 */
class PrismaticMedium : IMedium {
    private var layers: MutableList<LImg> = ArrayList()
    private var compositionImg: RawImage? = null
    private var compIsBuilt = false
    private var compRect: Rect? = null
    private val context: ImageWorkspace

    override val width: Int
        get() {
            buildComposition()
            return compRect!!.width
        }

    override val height: Int
        get() {
            buildComposition()
            return compRect!!.height
        }

    override val dynamicX: Int
        get() {
            buildComposition()
            return compRect!!.x
        }

    override val dynamicY: Int
        get() {
            buildComposition()
            return compRect!!.y
        }

    /** Note: the LImg's it returns are copies.  Changing the internal data will
     * not change the PrismaticImage's data, but drawing to the image WILL effect
     * the PrismaticImage.  */
    val colorLayers: List<LImg>
        get() {
            val list = ArrayList<LImg>(layers.size)

            for (limg in layers) {
                list.add(LImg(limg, false))
            }

            return list
        }

    internal var flushed = false

    override val type: IMedium.InternalImageTypes
        get() = IMedium.InternalImageTypes.PRISMATIC

    class LImg(
            val color: Int,
            val img: DynamicImage
    ) {
        constructor(other: LImg, deepcopy: Boolean) : this(
                other.color,
                if( deepcopy) other.img.deepCopy() else other.img
        ){}

        constructor(other: LImg, deepcopy: Boolean, copyForSave_IGNORED: Boolean) :this(
                other.color,
                if( deepcopy) other.img.deepCopy() else other.img
        ){
            //img = HybridUtil.copyForSaving(other.img);
        }
    }

    constructor(context: ImageWorkspace) {
        this.context = context
    }

    constructor(toImport: List<LImg>, context: ImageWorkspace) {
        this.context = context
        for (limg in toImport) {
            layers.add(LImg(limg, false))
        }
    }

    private fun buildComposition() {
        if (compIsBuilt)
            return

        var r = Rect(0, 0, 0, 0)
        for (img in layers)
            r = r.union(img.img.xOffset, img.img.yOffset, img.img.width, img.img.height)
        compRect = r

        if (compositionImg != null)
            compositionImg!!.flush()
        if (compRect == null || compRect!!.isEmpty) {
            compositionImg = HybridHelper.createNillImage()
        } else {
            compositionImg = HybridHelper.createImage(r.width, r.height)
            val gc = compositionImg!!.graphics
            for (img in layers) {
                gc.drawImage(img.img.base, img.img.xOffset - r.x, img.img.yOffset - r.y)
            }
        }

        compIsBuilt = true
    }

    fun drawBehind(gc: GraphicsContext, color: Int) {
        for (img in layers) {
            gc.drawImage(img.img.base, img.img.xOffset - compRect!!.x, img.img.yOffset - compRect!!.y)
            if (img.color and 0xFFFFFF == color and 0xFFFFFF) return
        }
    }

    fun drawFront(gc: GraphicsContext, color: Int) {
        var drawing = false
        for (img in layers) {
            if (drawing)
                gc.drawImage(img.img.base, img.img.xOffset - compRect!!.x, img.img.yOffset - compRect!!.y)
            else if (img.color and 0xFFFFFF == color and 0xFFFFFF)
                drawing = true
        }

    }

    override fun build(building: BuildingMediumData): BuiltMediumData {
        return PrismaticBuiltImageData(building)
    }

    override fun dupe(): IMedium {
        val pii = PrismaticMedium(context)

        pii.layers = ArrayList(this.layers.size)
        for (img in this.layers) {
            pii.layers.add(LImg(img, true))
        }
        return pii
    }

    override fun copyForSaving(): IMedium {
        val pii = PrismaticMedium(context)

        pii.layers = ArrayList(this.layers.size)
        for (img in this.layers) {
            pii.layers.add(LImg(img, true, true))
        }
        return pii
    }

    fun moveLayer(draggingFromIndex: Int, draggingToIndex: Int) {
        layers.add(draggingToIndex, layers.removeAt(draggingFromIndex))
        compIsBuilt = false
    }

    override fun flush() {
        if (!flushed) {
            for (img in layers)
                img.img.flush()
            if (compositionImg != null)
                compositionImg!!.flush()
            flushed = true
        }
    }

    @Throws(Throwable::class)
    protected fun finalize() {
        flush()
    }

    override fun readOnlyAccess(): IImage {
        buildComposition()
        return compositionImg!!
    }

    inner class PrismaticBuiltImageData constructor(building: BuildingMediumData) : BuiltMediumData(building) {
        private val workingColor: Int = building.color

        override val drawTrans: MatTrans get() = MatTrans()
        override val drawWidth: Int get() = context.width
        override val drawHeight: Int get() = context.height

        override val sourceTransform: MatTrans get() = trans
        override val sourceWidth: Int get() = width
        override val sourceHeight: Int get() = height

        override fun _doOnGC(doer: DoerOnGC) {
            prepareLImg().img.doOnGC(doer, trans)
        }

        override fun _doOnRaw(doer: DoerOnRaw) {
            prepareLImg().img.doOnRaw(doer, trans)
        }

        private fun prepareLImg(): LImg {
            val optionalLImg = layers.stream().filter { limg -> limg.color == workingColor }.findAny()
            val colorLayer: LImg
            if (optionalLImg.isPresent)
                colorLayer = optionalLImg.get()
            else {
                colorLayer = LImg(
                        workingColor,
                        DynamicImage(context, null, 0, 0))
                layers.add(colorLayer)
            }
            return colorLayer
        }
    }

    override fun getImageDrawer(building: BuildingMediumData): IImageDrawer {
        return DefaultImageDrawer(this, building)
    }
}
