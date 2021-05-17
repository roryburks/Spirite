package spirite.base.imageData.mediums

import rb.extendo.dataStructures.SinglyList
import rb.glow.Colors
import rb.glow.IGraphicsContext
import rb.glow.img.RawImage
import rb.vectrix.linear.ITransformF
import rb.vectrix.linear.ImmutableTransformF
import rb.glow.SColor
import spirite.base.imageData.MImageWorkspace
import spirite.base.imageData.MMediumRepository
import spirite.base.imageData.drawer.DefaultImageDrawer
import spirite.base.imageData.mediums.IImageMedium.ShiftedImage
import spirite.base.imageData.mediums.MediumType.FLAT

/***
 * A FlatMedium is a thin wrapper for a RawImage, exposing its functionality to the program.
 */
class FlatMedium(
        val image: RawImage,
        val mediumRepo: MMediumRepository
) : IImageMedium {
    override val x: Int get() = 0
    override val y: Int get() = 0
    override val width: Int get() = image.width
    override val height: Int get() = image.height
    override val type: MediumType get() = FLAT

    override fun getColor(x: Int, y: Int): SColor {
        return if( x < 0 || y < 0 || x >= width || y >= width) return Colors.TRANSPARENT
        else image.getColor(x,y)
    }

    override fun getImageDrawer(arranged: ArrangedMediumData) = DefaultImageDrawer(arranged)

    override fun getImages() = SinglyList(ShiftedImage(image))

    override fun dupe(workspace: MImageWorkspace) = FlatMedium(image.deepCopy(),workspace.mediumRepository)

    override fun flush() { image.flush() }

    override fun build(arranged: ArrangedMediumData): BuiltMediumData = FlatBuiltMediumData(arranged)

    inner class FlatBuiltMediumData(arranged: ArrangedMediumData) : BuiltMediumData(arranged, mediumRepo) {
        private val image = this@FlatMedium.image
        override val width: Int get() = image.width
        override val height: Int get() = image.height

        override val tMediumToComposite: ITransformF get() = ImmutableTransformF.Identity
        override val tWorkspaceToComposite: ITransformF by lazy { arranged.tMediumToWorkspace.invert() ?: ImmutableTransformF.Identity }

        override fun _drawOnComposite(doer: (IGraphicsContext) -> Unit) {
            val gc = image.graphics
            gc.transform = tWorkspaceToComposite
            doer.invoke( gc)
        }
        override fun _rawAccessComposite(doer: (RawImage) -> Unit) {
            doer.invoke( image)
        }
    }
}