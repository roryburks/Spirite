package spirite.base.imageData.mediums

import spirite.base.graphics.GraphicsContext
import spirite.base.graphics.RawImage
import spirite.base.graphics.RenderRubric
import spirite.base.imageData.mediums.IMedium.MediumType
import spirite.base.imageData.mediums.IMedium.MediumType.FLAT
import spirite.base.imageData.mediums.drawer.IImageDrawer
import spirite.base.util.linear.Transform

/***
 * A FlatMedium is a thin wrapper for a RawImage, exposing its functionality to the program.
 */
class FlatMedium(
        val image: RawImage
) : IMedium {
    override val width: Int get() = image.width
    override val height: Int get() = image.height
    override val type: MediumType get() = FLAT


    override fun getImageDrawer(arranged: ArrangedMediumData): IImageDrawer {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun render( gc: GraphicsContext, render: RenderRubric?) {
        gc.renderImage(image, 0, 0, render)
    }

    override fun dupe() = FlatMedium(image.deepCopy())

    override fun flush() { image.flush() }

    override fun build(arranged: ArrangedMediumData): BuiltMediumData = FlatBuiltMediumData(arranged)

    inner class FlatBuiltMediumData(arranged: ArrangedMediumData) : BuiltMediumData(arranged) {
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