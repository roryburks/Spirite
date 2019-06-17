package spirite.base.imageData.mediums

import rb.extendo.dataStructures.SinglyList
import rb.glow.RawImage
import rb.glow.color.Colors
import rb.glow.color.SColor
import rb.vectrix.linear.ITransformF
import rb.vectrix.linear.ImmutableTransformF
import spirite.base.graphics.DynamicImage
import rb.glow.GraphicsContext
import spirite.base.imageData.MImageWorkspace
import spirite.base.imageData.MMediumRepository
import spirite.base.imageData.drawer.DefaultImageDrawer
import spirite.base.imageData.mediums.IImageMedium.ShiftedImage
import spirite.base.imageData.mediums.MediumType.DYNAMIC


/***
 * A Dynamic Internal Image is a kind of image that automatically resizes itself
 * to its content bounds as it is drawn on top of.  This slightly increases the
 * time it takes to commit an image change, but substantially reduces memory overhead as well as
 * the number of pixels pushed to constantly re-draw the Workspace.
 */
class DynamicMedium
constructor(
        val workspace: MImageWorkspace,
        val image: DynamicImage = DynamicImage())
    : IImageMedium
{
    val mediumRepo: MMediumRepository get() = workspace.mediumRepository

    override val width: Int get() = image.width
    override val height: Int get() = image.height
    override val x: Int get() = image.xOffset
    override val y: Int get() = image.yOffset
    override val type: MediumType get() = DYNAMIC

    override fun getColor(x: Int, y: Int): SColor {
        return if( x < this.x || y < this.y || x >= this.x + width || y >= this.y + height) return Colors.TRANSPARENT
            else image.base?.getColor(x-this.x,y-this.y) ?: Colors.TRANSPARENT
    }

    override fun build(arranged: ArrangedMediumData) = DynamicBuiltImageData(arranged)

    override fun getImageDrawer(arranged: ArrangedMediumData) = DefaultImageDrawer(arranged)

    override fun getImages() : List<ShiftedImage> {
        val img = image.base
        if( img == null) return emptyList()
        else return SinglyList(ShiftedImage(img, x, y))
    }

    override fun dupe(workspace: MImageWorkspace) = DynamicMedium(workspace, image.deepCopy())

    override fun flush() {
        image.flush()
    }

    inner class DynamicBuiltImageData(arranged: ArrangedMediumData) : BuiltMediumData(arranged, mediumRepo) {
        override val width: Int get() = workspace.width
        override val height: Int get() = workspace.height
        override val tMediumToComposite: ITransformF get() = arranged.tMediumToWorkspace
        override val tWorkspaceToComposite: ITransformF get() = ImmutableTransformF.Identity

        override fun _drawOnComposite(doer: (GraphicsContext) -> Unit) {
            image.drawToImage(workspace.width,workspace.height, arranged.tMediumToWorkspace)
                { raw -> doer.invoke(raw.graphics)}
        }

        override fun _rawAccessComposite(doer: (RawImage) -> Unit) {
            image.drawToImage(workspace.width, workspace.height, arranged.tMediumToWorkspace)
                { raw -> doer.invoke(raw)}
        }
    }
}