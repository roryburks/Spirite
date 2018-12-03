package spirite.gui.components.basic

import spirite.base.graphics.IImage
import spirite.base.util.Colors
import rb.vectrix.mathUtil.ceil
import spirite.base.util.delegates.OnChangeDelegate
import rb.vectrix.mathUtil.f
import spirite.hybrid.Hybrid
import spirite.pc.graphics.ImageBI
import spirite.pc.gui.basic.SwComponent
import spirite.pc.gui.jcolor
import java.awt.Graphics
import java.awt.Image
import javax.swing.JPanel

interface IImageBox : IComponent {
    var stretch: Boolean
    var checkeredBackground: Boolean

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
        imp.repaint()
    }

    override var stretch by OnChangeDelegate(true ) {redraw()}
    override var checkeredBackground by OnChangeDelegate(false){redraw()}

    private class SwImageBoxImp() : JPanel() {
        lateinit var context : SwImageBox

        init {
            isOpaque = false
        }

        override fun paintComponent(g: Graphics) {
            if( context.checkeredBackground)
            {
                val s = 6
                val tx = (width-1)/s + 1
                val ty = (height-1)/s + 1
                for(dx in 0..tx) {
                    for( dy in 0..ty) {
                        g.color = if((dx + dy) % 2 == 0) Colors.LIGHT_GRAY.jcolor else Colors.GRAY.jcolor
                        g.fillRect(dx*s,dy*s,s,s)
                    }
                }
            }
            when(context.stretch) {
                true -> g.drawImage( context.img, 0, 0, width, height, null)
                false -> g.drawImage( context.img, 0, 0, null)
            }
        }
    }

}