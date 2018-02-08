package spirite.pc.graphics

import spirite.base.graphics.GraphicsContext
import spirite.base.graphics.RawImage
import spirite.base.util.Color

class ImageBI : RawImage {
    override fun getARGB(x: Int, y: Int): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getColor(x: Int, y: Int): Color {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override val graphics: GraphicsContext
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

    override val width: Int
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    override val height: Int
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    override val byteSize: Int
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

    override fun flush() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun deepCopy(): RawImage {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}