package spirite.base.imageData.selection

import rb.glow.IGraphicsContext
import rb.glow.img.IImage
import rb.vectrix.linear.ITransformF
import rb.vectrix.mathUtil.d
import spirite.base.graphics.drawer.IImageDrawer
import spirite.base.graphics.drawer.LiftedImageDrawer
import spirite.base.imageData.IImageWorkspace
import spirite.base.util.linear.Rect
import spirite.base.util.linear.RectangleUtil
import spirite.sguiHybrid.Hybrid

class LiftedImageData(val image: IImage): ILiftedData {
    override fun draw(gc: IGraphicsContext) {
        gc.renderImage(image, 0.0, 0.0)
    }

    override fun bake(transform: ITransformF): ILiftedData {
        // Bakes the rotation and scale, spits out the translation
        val bakedArea = RectangleUtil.circumscribeTrans(Rect(image.width, image.height),transform)
        val newImage = Hybrid.imageCreator.createImage(bakedArea.width, bakedArea.height)
        val gc = newImage.graphics
        gc.transform = transform
        gc.preTranslate(-bakedArea.x.d, -bakedArea.y.d)
        gc.renderImage(image, 0.0, 0.0)
        return LiftedImageData(newImage)
    }

    override fun getImageDrawer(workspace: IImageWorkspace) : IImageDrawer = LiftedImageDrawer(workspace)

    override val width: Int get() = image.width
    override val height: Int get() = image.height

}