package spirite.base.imageData.mediums.magLev

import rb.vectrix.linear.*
import spirite.base.graphics.DynamicImage
import spirite.base.graphics.GraphicsContext
import spirite.base.graphics.RawImage
import spirite.base.graphics.RenderRubric
import spirite.base.imageData.IImageWorkspace
import spirite.base.imageData.MMediumRepository
import spirite.base.imageData.mediums.ArrangedMediumData
import spirite.base.imageData.mediums.BuiltMediumData
import spirite.base.imageData.mediums.IMedium
import spirite.base.imageData.mediums.IMedium.MediumType
import spirite.base.imageData.mediums.IMedium.MediumType.MAGLEV
import spirite.base.imageData.undo.ImageAction
import spirite.base.util.Colors
import spirite.pc.gui.SColor

interface IMaglevThing {
    fun transformPoints( lambda: (Vec3f)->(Vec3f))
    fun dupe() : IMaglevThing
    fun draw(built: BuiltMediumData)
}

class MaglevMedium
private constructor(
        val workspace: IImageWorkspace,
        private val mediumRepo: MMediumRepository,
        private val things: MutableList<IMaglevThing>,
        private val builtImage : DynamicImage = DynamicImage()) // Maybe expose this so
    :IMedium
{
    constructor(
            workspace: IImageWorkspace,
            mediumRepo: MMediumRepository,
            things: List<IMaglevThing>? = null)
            : this(workspace, mediumRepo, things?.toMutableList() ?: mutableListOf())


    // Note: since ImageActions are inherently designed to be destructive, i.e. not-undoable, we
    //  do not need to worry about removing Things from the Medium, instead the duplication of medium snapshots
    //  handles the thing lifecycle w.r.t. the undo engine
    internal fun addThing(thing : IMaglevThing, arranged: ArrangedMediumData, description: String) {
        things.add(thing)

        arranged.handle.workspace.undoEngine.performAndStore(object : ImageAction(arranged){
            override val description: String get() = description
            override fun performImageAction(built: BuiltMediumData) = thing.draw(built)
        })
    }

    internal fun applyTrasformation( arranged: ArrangedMediumData, description: String, lambda: (Vec3f) -> Vec3f) {
        things.forEach { it.transformPoints(lambda) }

        arranged.handle.workspace.undoEngine.performAndStore(object : ImageAction(arranged) {
            override val description: String get() = description
            override val isHeavy: Boolean get() = true

            override fun performImageAction(built: BuiltMediumData) {
                built.rawAccessComposite {it.graphics.clear()}
                things.forEach { it.draw(built) }
            }
        })
    }

    // region IMedium
    override val x: Int get() = builtImage.xOffset
    override val y: Int get() = builtImage.yOffset
    override val width: Int get() = builtImage.width
    override val height: Int get() = builtImage.height
    override val type: MediumType get() = MAGLEV

    override fun render(gc: GraphicsContext, render: RenderRubric?) {
        builtImage.base?.also { gc.renderImage(it, x, y, render)}
    }

    override fun getColor(x: Int, y: Int): SColor {
        val img = builtImage
        return if( x < this.x || y < this.y || x >= this.x + width || y >= this.y + height) return Colors.TRANSPARENT
        else img.base?.getColor(x-this.x,y-this.y) ?: Colors.TRANSPARENT
    }

    override fun build(arranged: ArrangedMediumData) = MaglevBuiltMediumData(arranged)

    override fun getImageDrawer(arranged: ArrangedMediumData) = MaglevImageDrawer(arranged, this)

    override fun dupe() = MaglevMedium(workspace, mediumRepo, things, this.builtImage.deepCopy())
    override fun flush() { builtImage.flush() }
    // endregion

    // region BuildMediumData

    /***
     * Note: W.R.T actual draws, MaglevMediums behave very much like Dynamic Mediums, the only difference being
     * that the record of what was done to create the MaglevMedium is stored within it under a system that can be
     * transformed and re-rendered.
     */
    inner class MaglevBuiltMediumData(arranged: ArrangedMediumData) : BuiltMediumData(arranged, mediumRepo)
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