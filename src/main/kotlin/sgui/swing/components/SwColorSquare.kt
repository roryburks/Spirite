package sgui.swing.components

import rb.owl.bindable.addObserver
import rb.glow.SColor
import sgui.core.components.ColorSquareNonUI
import sgui.core.components.IColorSquare
import sgui.core.components.IColorSquareNonUI
import sgui.swing.jcolor
import sgui.swing.scolor
import sguiSwing.components.ISwComponent
import sguiSwing.components.SwComponent
import java.awt.Graphics
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JColorChooser

class SwColorSquare
private constructor(
    defaultColor: SColor,
    private val imp : SwColorSquareImp
)
    : IColorSquare, ISwComponent by SwComponent(imp), IColorSquareNonUI by ColorSquareNonUI(defaultColor)
{
    constructor(defaultColor: SColor) : this(defaultColor, SwColorSquareImp())
    init {
        imp.context = this
        onMouseClick += {
            if( active) {
                this.color = JColorChooser.showDialog(
                        imp,
                        "Choose Background Color",
                        defaultColor.jcolor)?.scolor ?: this.color
            }
        }
        colorBind.addObserver { new, _ -> imp.background = new.jcolor}
    }

    private class SwColorSquareImp : SJPanel() {
        var context : SwColorSquare? = null

        init {
            this.isOpaque = false
            addMouseListener(object : MouseAdapter() {
                override fun mousePressed(e: MouseEvent?) {requestFocus()}
            })
        }

        override fun paintComponent(g: Graphics) {
            val ctx = context?: return
            g.color = ctx.color.jcolor
            g.fillRect(0, 0, width, height)
        }
    }
}