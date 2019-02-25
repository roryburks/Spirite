package spirite.base.imageData.mediums.magLev

import rb.vectrix.linear.ITransformF
import rb.vectrix.linear.ImmutableTransformF
import rb.vectrix.linear.Vec2f
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
    fun transformPoints( lambda: (Vec2f)->(Vec2f))
    fun dupe() : IMaglevThing
    fun draw(built: BuiltMediumData)
}

class MaglevMedium(
        val workspace: IImageWorkspace,
        private val mediumRepo: MMediumRepository,
        things: List<IMaglevThing>? = null
)
    :IMedium
{
    private val builtImage = DynamicImage()
    private val things = mutableListOf<IMaglevThing>()

    init {
        things?.also { this.things.addAll(it) }
    }

    internal fun addThing(thing : IMaglevThing, arranged: ArrangedMediumData, description: String) {
        things.add(thing)

        arranged.handle.workspace.undoEngine.performAndStore(object : ImageAction(arranged){
            override val description: String get() = description
            override fun performImageAction(built: BuiltMediumData) = thing.draw(built)
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

    override fun build(arranged: ArrangedMediumData): BuiltMediumData {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getImageDrawer(arranged: ArrangedMediumData) = MaglevImageDrawer(arranged)

    override fun dupe() = MaglevMedium(workspace, mediumRepo, things)
    override fun flush() { builtImage.flush() }
    // endregion

    // region BuildMediumData

    /***
     * Note: W.R.T actual draws, MaglevMediums behave very much like Dynamic Mediums, the only difference being
     * that the record of what was done to create the MaglevMedium is stored within it under a system that can be
     * transformed and re-rendered.
     */
    inner class MaglevArrangedMediumData(arranged: ArrangedMediumData) : BuiltMediumData(arranged, mediumRepo)
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