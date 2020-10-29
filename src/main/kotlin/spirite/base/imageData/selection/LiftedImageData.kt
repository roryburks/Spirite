package spirite.base.imageData.selection

import rb.glow.GraphicsContext_old
import rb.glow.img.IImage
import rb.vectrix.linear.ITransformF
import rb.vectrix.mathUtil.f
import spirite.base.imageData.IImageWorkspace
import spirite.base.imageData.drawer.IImageDrawer
import spirite.base.imageData.drawer.LiftedImageDrawer
import spirite.base.util.linear.Rect
import spirite.base.util.linear.RectangleUtil
import sguiSwing.hybrid.Hybrid

class LiftedImageData(val image: IImage): ILiftedData {
    override fun draw(gc: GraphicsContext_old) {
        gc.renderImage(image, 0, 0)
    }

    override fun bake(transform: ITransformF): ILiftedData {
        // Bakes the rotation and scale, spits out the translation
        val bakedArea = RectangleUtil.circumscribeTrans(Rect(image.width, image.height),transform)
        val newImage = Hybrid.imageCreator.createImage(bakedArea.width, bakedArea.height)
        val gc = newImage.graphicsOld
        gc.transform = transform
        gc.preTranslate(-bakedArea.x.f, -bakedArea.y.f)
        gc.renderImage(image, 0, 0)
        return LiftedImageData(newImage)
    }

    override fun getImageDrawer(workspace: IImageWorkspace) : IImageDrawer = LiftedImageDrawer(workspace)

    override val width: Int get() = image.width
    override val height: Int get() = image.height

}