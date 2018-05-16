package spirite.base.imageData.selection

import spirite.base.graphics.GraphicsContext
import spirite.base.graphics.IImage
import spirite.base.imageData.IImageWorkspace
import spirite.base.imageData.drawer.IImageDrawer
import spirite.base.imageData.drawer.LiftedImageDrawer
import spirite.base.util.MathUtil
import spirite.base.util.f
import spirite.base.util.linear.Rect
import spirite.base.util.linear.Transform
import spirite.hybrid.Hybrid

class LiftedImageData(val image: IImage): ILiftedData {
    override fun draw(gc: GraphicsContext) {
        gc.renderImage(image, 0, 0)
    }

    override fun bake(transform: Transform): Pair<ILiftedData, Transform> {
        // Bakes the rotation and scale, spits out the translation
        val bakedArea = MathUtil.circumscribeTrans(Rect(image.width, image.height),transform)
        val newTrans = Transform.TranslationMatrix(bakedArea.x.f, bakedArea.y.f)
        val newImage = Hybrid.imageCreator.createImage(bakedArea.width, bakedArea.height)
        val gc = newImage.graphics
        gc.transform = transform
        gc.renderImage(image, 0, 0)

        return Pair(LiftedImageData(newImage),newTrans)
    }

    override fun getImageDrawer(workspace: IImageWorkspace) : IImageDrawer = LiftedImageDrawer(workspace)

    override val width: Int get() = image.width
    override val height: Int get() = image.height

}