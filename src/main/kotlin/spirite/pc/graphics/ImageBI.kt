package spirite.pc.graphics

import spirite.base.graphics.GraphicsContext
import spirite.base.graphics.IDrawer
import spirite.base.graphics.RawImage
import spirite.base.util.Color
import spirite.base.util.ColorARGB32Normal
import spirite.base.util.ColorARGB32Premultiplied
import java.awt.image.BufferedImage

class ImageBI(
        val bi: BufferedImage
) : RawImage {
    override fun getARGB(x: Int, y: Int): Int = bi.getRGB(x,y)

    override fun getColor(x: Int, y: Int): Color {
        return when( bi.isAlphaPremultiplied) {
            true -> ColorARGB32Premultiplied(bi.getRGB(x,y))
            false -> ColorARGB32Normal(bi.getRGB(x,y))
        }
    }

    override val graphics: GraphicsContext
        get() = TODO("not implemented")
    override val drawer: IDrawer
        get() = TODO("not implemented")

    override val width: Int get() = bi.width
    override val height: Int get() = bi.height
    override val byteSize: Int get() = 4

    override fun flush() = bi.flush()

    override fun deepCopy(): RawImage {
        TODO("not implemented")
    }
}