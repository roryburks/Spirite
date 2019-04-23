package spirite.base.imageData.mediums.magLev

import rb.extendo.dataStructures.SinglyList
import rb.vectrix.linear.ITransformF
import rb.vectrix.linear.ImmutableTransformF
import spirite.base.graphics.DynamicImage
import spirite.base.graphics.GraphicsContext
import spirite.base.graphics.RawImage
import spirite.base.graphics.RenderRubric
import spirite.base.imageData.MImageWorkspace
import spirite.base.imageData.MediumHandle
import spirite.base.imageData.mediums.*
import spirite.base.imageData.mediums.MediumType.MAGLEV
import spirite.base.util.Colors
import spirite.pc.gui.SColor


class MaglevMedium
constructor(
        private val workspace: MImageWorkspace,
        internal val things: MutableList<IMaglevThing>,
        val builtImage : DynamicImage)
    :IMedium, IImageMedium
{
    constructor(
            workspace: MImageWorkspace,
            things: List<IMaglevThing>? = null)
            : this(workspace, things?.toMutableList() ?: mutableListOf(), DynamicImage())

    fun getThings() = things.toList()

    fun build(handle: MediumHandle)
    {
        val arranged = ArrangedMediumData(handle, 0f, 0f)
        val built = build(arranged)
        things.forEach { it.draw(built) }
    }

    // Note: since ImageActions are inherently designed to be destructive, i.e. not-undoable, we
    //  do not need to worry about removing Things from the Medium, instead the duplication of medium snapshots
    //  handles the thing lifecycle w.r.t. the undo engine
    internal fun addThing(thing : IMaglevThing, arranged: ArrangedMediumData, description: String) {
        arranged.handle.workspace.undoEngine.performAndStore(object : MaglevImageAction(arranged){
            override fun performMaglevAction(built: BuiltMediumData, maglev: MaglevMedium) {
                maglev.things.add(thing)
                thing.draw(built)
            }
            override val description: String get() = description
        })
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
            things.map { it.dupe() }.toMutableList(),
            this.builtImage.deepCopy())
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

        override fun _drawOnComposite(doer: (GraphicsContext) -> Unit) {
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