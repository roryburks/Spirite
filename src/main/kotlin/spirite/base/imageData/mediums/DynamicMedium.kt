package spirite.base.imageData.mediums

import spirite.base.graphics.*
import spirite.base.imageData.IImageWorkspace
import spirite.base.imageData.mediums.IMedium.MediumType
import spirite.base.imageData.mediums.IMedium.MediumType.DYNAMIC
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

    override fun build(building: BuildingMediumData) = DynamicBuiltImageData(building)

    override fun getImageDrawer(building: BuildingMediumData): IImageDrawer {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun draw(gc: GraphicsContext, render: RenderRhubric) {
        val img = image.base
        if( img != null) {
            gc.renderImage(img, image.xOffset, image.yOffset, render)
        }
    }

    override fun dupe() = DynamicMedium(workspace, image.deepCopy())

    override fun flush() {
        image.flush()
    }

    inner class DynamicBuiltImageData( building: BuildingMediumData) : BuiltMediumData(building) {
        override val width: Int get() = workspace.width
        override val height: Int get() = workspace.height
        override val tCompositeToWorkspace: Transform get() = Transform.IdentityMatrix

        override fun _doOnGC(doer: (GraphicsContext) -> Unit) {
            image.drawToImage( {raw -> doer.invoke(raw.graphics)},
                    workspace.width, workspace.height, building.tMediumToWorkspace)
        }

        override fun _doOnRaw(doer: (RawImage, tWorkspaceToRaw: Transform) -> Unit) {
            image.drawToImage( {raw -> doer.invoke(raw, building.tMediumToWorkspace.invert())},
                    workspace.width, workspace.height, building.tMediumToWorkspace)
        }
    }
}