package spirite.gui.components.basic

import spirite.base.graphics.IImage
import spirite.base.util.delegates.OnChangeDelegate
import spirite.hybrid.Hybrid
import spirite.pc.graphics.ImageBI
import spirite.pc.gui.basic.SwComponent
import java.awt.Graphics
import java.awt.Image
import javax.swing.JPanel

interface IImageBox : IComponent {
    var stretch: Boolean

    fun setImage( img: IImage)
}

class SwImageBox
private constructor(img: IImage, private val imp : SwImageBoxImp)
    : IImageBox, IComponent by SwComponent(imp)
{
    constructor(img: IImage) : this(img, SwImageBoxImp())
    init { imp.context = this }

    private var img: Image = Hybrid.imageConverter.convert<ImageBI>(img).bi

    override fun setImage(img: IImage) {
        this.img = Hybrid.imageConverter.convert<ImageBI>(img).bi
        redraw()
    }

    override var stretch by OnChangeDelegate(true ) {redraw()}

    private class SwImageBoxImp() : JPanel() {
        lateinit var context : SwImageBox

        init {
            isOpaque = false
        }

        override fun paintComponent(g: Graphics) {
            when(context.stretch) {
                true -> g.drawImage( context.img, 0, 0, width, height, null)
                false -> g.drawImage( context.img, 0, 0, null)
            }
        }
    }

}