package spirite.base.imageData.selection

import spirite.base.graphics.GraphicsContext
import spirite.base.graphics.IImage

class LiftedImageData(val image: IImage): ILiftedData {
    override fun draw(gc: GraphicsContext) {
        gc.renderImage(image, 0, 0)
    }

    override val width: Int get() = image.width
    override val height: Int get() = image.height

}