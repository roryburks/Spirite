package spirite.pc.graphics

import spirite.base.graphics.GraphicsContext
import spirite.base.graphics.RawImage

class ImageBI : RawImage {
    override val graphics: GraphicsContext
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

    override val width: Int
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    override val height: Int
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    override val isGLOriented: Boolean
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    override val byteSize: Int
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

    override fun flush() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun deepCopy(): RawImage {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getRGB(x: Int, y: Int): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}