package spirite.base.imageData.mediums

import spirite.base.graphics.GraphicsContext
import spirite.base.graphics.RawImage
import spirite.base.graphics.RenderRubric
import spirite.base.imageData.MMediumRepository
import spirite.base.imageData.drawer.DefaultImageDrawer
import spirite.base.imageData.mediums.IMedium.MediumType
import spirite.base.imageData.mediums.IMedium.MediumType.FLAT
import spirite.base.util.linear.Transform

/***
 * A FlatMedium is a thin wrapper for a RawImage, exposing its functionality to the program.
 */
class FlatMedium(
        val image: RawImage,
        val mediumRepo: MMediumRepository
) : IMedium {
    override val x: Int get() = 0
    override val y: Int get() = 0
    override val width: Int get() = image.width
    override val height: Int get() = image.height
    override val type: MediumType get() = FLAT


    override fun getImageDrawer(arranged: ArrangedMediumData) = DefaultImageDrawer(arranged)

    override fun render( gc: GraphicsContext, render: RenderRubric?) {
        gc.renderImage(image, 0, 0, render)
    }

    override fun dupe() = FlatMedium(image.deepCopy(),mediumRepo)

    override fun flush() { image.flush() }

    override fun build(arranged: ArrangedMediumData): BuiltMediumData = FlatBuiltMediumData(arranged)

    inner class FlatBuiltMediumData(arranged: ArrangedMediumData) : BuiltMediumData(arranged, mediumRepo) {
        private val image = this@FlatMedium.image
        override val width: Int get() = image.width
        override val height: Int get() = image.height

        override val tMediumToComposite: Transform get() = Transform.IdentityMatrix
        override val tWorkspaceToComposite: Transform by lazy { arranged.tMediumToWorkspace.invert() }

        override fun _drawOnComposite(doer: (GraphicsContext) -> Unit) {
            val gc = image.graphics
            gc.transform = tWorkspaceToComposite
            doer.invoke( gc)
        }
        override fun _rawAccessComposite(doer: (RawImage) -> Unit) {
            doer.invoke( image)
        }
    }
}