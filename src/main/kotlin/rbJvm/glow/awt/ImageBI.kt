package rbJvm.glow.awt

import rb.glow.*
import rb.glow.img.RawImage
import java.awt.image.BufferedImage

class ImageBI(
    val bi: BufferedImage
) : RawImage {
    override fun getARGB(x: Int, y: Int): Int = bi.getRGB(x,y)

    override fun getColor(x: Int, y: Int): Color {
        return when( bi.isAlphaPremultiplied) {
            true -> ColorARGB32Premultiplied(bi.getRGB(x, y))
            false -> ColorARGB32Normal(bi.getRGB(x, y))
        }
    }

    override val graphics: IGraphicsContext get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

    override val width: Int get() = bi.width
    override val height: Int get() = bi.height
    override val byteSize: Int get() = 4

    override fun flush() = bi.flush()

    override fun deepCopy(): RawImage {
        TODO("not implemented")
    }
}