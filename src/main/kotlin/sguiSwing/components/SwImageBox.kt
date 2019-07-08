package sguiSwing.components

import rb.extendo.delegates.OnChangeDelegate
import rb.glow.IImage
import rb.glow.color.Colors
import rbJvm.glow.awt.ImageBI
import sgui.components.IComponent
import sgui.components.IImageBox
import sguiSwing.SwProvider
import sguiSwing.jcolor
import java.awt.Graphics
import java.awt.Image

class SwImageBox
private constructor(
        img: IImage?,
        private val imp : SwImageBoxImp)
    : IImageBox, IComponent by SwComponent(imp)
{
    init { imp.context = this }

    private var _img: Image? = img?.run { SwProvider.convertOrNull<ImageBI>(img)?.bi}
    override var stretch by OnChangeDelegate(true) { redraw() }
    override var checkeredBackground by OnChangeDelegate(false) { redraw() }

    constructor(img: IImage?) : this(img, SwImageBoxImp())

    override fun setImage(img: IImage?) {
        this._img = img?.run { SwProvider.convertOrNull<ImageBI>(img)?.bi}
        imp.repaint()
    }

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

            val img = context._img
            when {
                img == null -> {}
                context.stretch -> g.drawImage( img, 0, 0, width, height, null)
                else -> g.drawImage( img, 0, 0, null)
            }
        }
    }

}