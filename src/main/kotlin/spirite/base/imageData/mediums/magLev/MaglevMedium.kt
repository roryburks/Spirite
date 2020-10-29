package spirite.base.imageData.mediums.magLev

import rb.extendo.dataStructures.SinglyList
import rb.glow.GraphicsContext_old
import rb.glow.img.RawImage
import rb.glow.Colors
import rb.glow.IGraphicsContext
import rbJvm.glow.SColor
import rb.vectrix.linear.ITransformF
import rb.vectrix.linear.ImmutableTransformF
import spirite.base.graphics.DynamicImage
import spirite.base.imageData.MImageWorkspace
import spirite.base.imageData.MediumHandle
import spirite.base.imageData.mediums.*
import spirite.base.imageData.mediums.MediumType.MAGLEV


class MaglevMedium
constructor(
        val workspace: MImageWorkspace,
        things: Map<Int,IMaglevThing>?,
        val builtImage : DynamicImage,
        private var met: Int)
    :IMedium, IImageMedium
{
    internal val thingsMap = things?.toMutableMap() ?: mutableMapOf()

    constructor(
            workspace: MImageWorkspace,
            things: Map<Int,IMaglevThing>? = null)
            : this(workspace, things, DynamicImage(), things?.keys?.max()?.apply { this + 1 } ?: 0)
    constructor(
            workspace: MImageWorkspace,
            things: List<IMaglevThing>)
            : this(workspace, things.mapIndexed { i, thing -> Pair(i,thing) }.toMap(), DynamicImage(),  things.count())

    fun getThingsMap() : Map<Int,IMaglevThing> = thingsMap

    fun build(handle: MediumHandle)
    {
        val arranged = ArrangedMediumData(handle, 0f, 0f)
        val built = build(arranged)
        thingsMap.values.forEach { it.draw(built) }
    }

    // Note: since ImageActions are inherently designed to be destructive, i.e. not-undoable, we
    //  do not need to worry about removing Things from the Medium, instead the duplication of medium snapshots
    //  handles the thing lifecycle w.r.t. the undo engine
    internal fun addThing(thing : IMaglevThing, arranged: ArrangedMediumData, description: String)
            =   addThings(SinglyList(thing), arranged, description)
    internal fun addThings(things : List<IMaglevThing>, arranged: ArrangedMediumData, description: String) {
        arranged.handle.workspace.undoEngine.performAndStoreMaglevImageAction(arranged, description) {built, maglev ->
            things.forEach {
                maglev.thingsMap[maglev.met++] = it
                it.draw(built)
            }
        }
    }

    internal fun removeThings( things: Collection<IMaglevThing>, arranged: ArrangedMediumData, description: String) {
        //val toRemoveSet= things.toSet()
        arranged.handle.workspace.undoEngine.performAndStoreMaglevImageAction(arranged, description) {built, maglev ->
            maglev.thingsMap.values.removeAll(things)
            maglev.builtImage.flush()
            maglev.thingsMap.values.forEach {it.draw(built)  }
        }
    }

    // region IMedium
    override val x: Int get() = builtImage.xOffset
    override val y: Int get() = builtImage.yOffset
    override val width: Int get() = builtImage.width
    override val height: Int get() = builtImage.height
    override val type: MediumType get() = MAGLEV

    override fun getImages() = when( val img = builtImage.base) {
        null -> emptyList<IImageMedium.ShiftedImage>()
        else -> SinglyList(IImageMedium.ShiftedImage(img, x, y))
    }


    override fun getColor(x: Int, y: Int): SColor {
        val img = builtImage
        return if( x < this.x || y < this.y || x >= this.x + width || y >= this.y + height) return Colors.TRANSPARENT
        else img.base?.getColor(x-this.x,y-this.y) ?: Colors.TRANSPARENT
    }

    override fun build(arranged: ArrangedMediumData) = MaglevBuiltMediumData(arranged)

    override fun getImageDrawer(arranged: ArrangedMediumData) = MaglevImageDrawer(arranged, this)

    override fun dupe(workspace: MImageWorkspace) = MaglevMedium(
            workspace,
            thingsMap
                    .map { Pair(it.key, it.value.dupe()) }
                    .toMap(),
            this.builtImage.deepCopy(),
            met)
    override fun flush() { builtImage.flush() }
    // endregion

    // region BuildMediumData

    /***
     * Note: W.R.T actual draws, MaglevMediums behave very much like Dynamic Mediums, the only difference being
     * that the record of what was done to create the MaglevMedium is stored within it under a system that can be
     * transformed and re-rendered.
     */
    inner class MaglevBuiltMediumData(arranged: ArrangedMediumData) : BuiltMediumData(arranged, workspace.mediumRepository)
    {
        override val width: Int get() = workspace.width
        override val height: Int get() = workspace.height
        override val tMediumToComposite: ITransformF get() = arranged.tMediumToWorkspace
        override val tWorkspaceToComposite: ITransformF get() = ImmutableTransformF.Identity

        override fun _drawOnComposite(doer: (IGraphicsContext) -> Unit) {
            builtImage.drawToImage(workspace.width,workspace.height, arranged.tMediumToWorkspace)
            { raw -> doer.invoke(raw.graphics)}
        }

        override fun _rawAccessComposite(doer: (RawImage) -> Unit) {
            builtImage.drawToImage(workspace.width, workspace.height, arranged.tMediumToWorkspace)
            { raw -> doer.invoke(raw)}
        }

    }
    // endregion
}