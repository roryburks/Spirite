package rb.glow.gl

import rb.glow.gle.IGLEngine
import rb.glow.img.IImage

interface IGLImageConverter {
    fun convertToGL(image: IImage, gle: IGLEngine) : GLImage
}