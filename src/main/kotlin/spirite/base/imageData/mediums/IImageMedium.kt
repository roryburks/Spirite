package spirite.base.imageData.mediums

import spirite.base.graphics.GraphicsContext
import rb.glow.IImage
import spirite.base.graphics.RenderRubric

interface IImageMedium : IMedium{
    override fun render(gc: GraphicsContext, render: RenderRubric?) {
        getImages()
                .forEach {gc.renderImage(it.image, it.x, it.y, render)}
    }

    fun getImages() : List<ShiftedImage>

    data class ShiftedImage(
            val image : IImage,
            val x: Int = 0,
            val y: Int = 0)
}
