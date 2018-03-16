package spirite.base.imageData.mediums

import spirite.base.graphics.DynamicImage
import spirite.base.graphics.GraphicsContext
import spirite.base.graphics.RawImage
import spirite.base.graphics.RenderRubric
import spirite.base.imageData.IImageWorkspace
import spirite.base.imageData.mediums.IMedium.MediumType
import spirite.base.imageData.mediums.IMedium.MediumType.DYNAMIC
import spirite.base.imageData.mediums.drawer.DefaultImageDrawer
import spirite.base.imageData.mediums.drawer.IImageDrawer
import spirite.base.util.linear.Transform


/***
 * A Dynamic Internal Image is a kind of image that automatically resizes itself
 * to its content bounds as it is drawn on top of.  This slightly increases the
 * time it takes to commit an image change, but substantially reduces memory overhead as well as
 * the number of pixels pushed to constantly re-draw the Workspace.
 */
class DynamicMedium(
        val workspace: IImageWorkspace,
        val image: DynamicImage = DynamicImage()) : IMedium
{
    override val width: Int get() = image.width
    override val height: Int get() = image.height
    override val type: MediumType get() = DYNAMIC

    override fun build(arranged: ArrangedMediumData) = DynamicBuiltImageData(arranged)

    override fun getImageDrawer(arranged: ArrangedMediumData) = DefaultImageDrawer(arranged)

    override fun render( gc: GraphicsContext, render: RenderRubric?) {
        val img = image.base
        if( img != null) {
            gc.renderImage(img, image.xOffset, image.yOffset, render)
        }
    }

    override fun dupe() = DynamicMedium(workspace, image.deepCopy())

    override fun flush() {
        image.flush()
    }

    inner class DynamicBuiltImageData(arranged: ArrangedMediumData) : BuiltMediumData(arranged) {
        override val width: Int get() = workspace.width
        override val height: Int get() = workspace.height
        override val tMediumToComposite: Transform get() = arranged.tMediumToWorkspace
        override val tWorkspaceToComposite: Transform get() = Transform.IdentityMatrix

        override fun _drawOnComposite(doer: (GraphicsContext) -> Unit) {
            image.drawToImage( {raw -> doer.invoke(raw.graphics)},
                    workspace.width, workspace.height, arranged.tMediumToWorkspace)
        }

        override fun _rawAccessComposite(doer: (RawImage) -> Unit) {
            image.drawToImage( {raw -> doer.invoke(raw)},
                    workspace.width, workspace.height, arranged.tMediumToWorkspace)
        }
    }
}