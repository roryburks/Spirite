package spirite.base.imageData.mediums

import spirite.base.graphics.GraphicsContext
import spirite.base.graphics.IImage
import spirite.base.graphics.RawImage
import spirite.base.imageData.mediums.IMedium.MediumType
import spirite.base.imageData.mediums.IMedium.MediumType.FLAT
import spirite.base.imageData.mediums.drawer.IImageDrawer
import spirite.base.util.linear.Transform

class FlatMedium(
        private val rawImage: RawImage
) : IMedium {
    val image : IImage get() = rawImage

    override val width: Int get() = rawImage.width
    override val height: Int get() = rawImage.height
    override val type: MediumType get() = FLAT


    override fun getImageDrawer(building: BuildingMediumData): IImageDrawer {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun draw(gc: GraphicsContext) {
        gc.drawImage(rawImage, 0, 0)
    }

    override fun dupe() = FlatMedium(rawImage.deepCopy())

    override fun flush() { rawImage.flush() }

    override fun build(building: BuildingMediumData): BuiltMediumData = FlatBuiltMediumData(building)

    inner class FlatBuiltMediumData( building: BuildingMediumData) : BuiltMediumData(building) {
        override val width: Int get() = image.width
        override val height: Int get() = image.height
        override val tCompositeToWorkspace: Transform get() = building.mediumTransform
        override fun _doOnGC(doer: (GraphicsContext) -> Unit) { doer.invoke( rawImage.graphics) }
        override fun _doOnRaw(doer: (RawImage, tWorkspaceToRaw: Transform) -> Unit) {
            doer.invoke( rawImage, tCompositeToWorkspace.invert())
        }
    }
}