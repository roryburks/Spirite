package spirite.base.imageData.mediums

import spirite.base.graphics.*
import spirite.base.imageData.IImageWorkspace
import spirite.base.imageData.MMediumRepository
import spirite.base.imageData.drawer.DefaultImageDrawer
import spirite.base.imageData.mediums.IImageMedium.ShiftedImage
import spirite.base.imageData.mediums.IMedium.MediumType
import spirite.base.imageData.mediums.IMedium.MediumType.DYNAMIC
import rb.extendo.dataStructures.SinglyList
import rb.vectrix.linear.ITransformF
import rb.vectrix.linear.ImmutableTransformF


/***
 * A Dynamic Internal Image is a kind of image that automatically resizes itself
 * to its content bounds as it is drawn on top of.  This slightly increases the
 * time it takes to commit an image change, but substantially reduces memory overhead as well as
 * the number of pixels pushed to constantly re-draw the Workspace.
 */
class DynamicMedium(
        val workspace: IImageWorkspace,
        val image: DynamicImage = DynamicImage(),
        val mediumRepo: MMediumRepository)
    : IImageMedium
{
    override val width: Int get() = image.width
    override val height: Int get() = image.height
    override val x: Int get() = image.xOffset
    override val y: Int get() = image.yOffset
    override val type: MediumType get() = DYNAMIC

    override fun build(arranged: ArrangedMediumData) = DynamicBuiltImageData(arranged)

    override fun getImageDrawer(arranged: ArrangedMediumData) = DefaultImageDrawer(arranged)

    override fun getImages() : List<ShiftedImage> {
        val img = image.base
        if( img == null) return emptyList()
        else return SinglyList(ShiftedImage(img, x, y))
    }

    override fun dupe() = DynamicMedium(workspace, image.deepCopy(), mediumRepo)

    override fun flush() {
        image.flush()
    }

    inner class DynamicBuiltImageData(arranged: ArrangedMediumData) : BuiltMediumData(arranged, mediumRepo) {
        override val width: Int get() = workspace.width
        override val height: Int get() = workspace.height
        override val tMediumToComposite: ITransformF get() = arranged.tMediumToWorkspace
        override val tWorkspaceToComposite: ITransformF get() = ImmutableTransformF.Identity

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