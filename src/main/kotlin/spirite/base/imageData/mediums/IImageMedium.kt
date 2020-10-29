package spirite.base.imageData.mediums

import rb.glow.GraphicsContext_old
import rb.glow.IGraphicsContext
import rb.glow.img.IImage
import rb.glow.gle.RenderRubric
import rb.vectrix.mathUtil.d

interface IImageMedium : IMedium{
    override fun render(gc: IGraphicsContext, render: RenderRubric?) {
        getImages()
                .forEach {gc.renderImage(it.image, it.x.d, it.y.d, render)}
    }

    fun getImages() : List<ShiftedImage>

    data class ShiftedImage(
            val image : IImage,
            val x: Int = 0,
            val y: Int = 0)
}
