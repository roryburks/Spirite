package sgui.generic.components

import rb.extendo.delegates.OnChangeDelegate
import rb.glow.IImage
import sgui.generic.color.Colors
import spirite.hybrid.Hybrid
import spirite.pc.graphics.ImageBI
import sgui.swing.components.SJPanel
import sgui.swing.components.SwComponent
import spirite.pc.gui.jcolor
import java.awt.Graphics
import java.awt.Image

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

    override var stretch by OnChangeDelegate(true) { redraw() }
    override var checkeredBackground by OnChangeDelegate(false) { redraw() }

    private class SwImageBoxImp() : SJPanel() {
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