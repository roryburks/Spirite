package spirite.gui.components.basic

import rb.owl.bindable.Bindable
import rb.owl.bindable.addObserver
import spirite.pc.gui.SColor
import spirite.pc.gui.basic.ISwComponent
import spirite.pc.gui.basic.SJPanel
import spirite.pc.gui.basic.SwComponent
import spirite.pc.gui.jcolor
import spirite.pc.gui.scolor
import java.awt.Graphics
import javax.swing.JColorChooser
import javax.swing.JPanel

interface IColorSquareNonUI {
    val colorBind : Bindable<SColor>
    var color : SColor
    var active : Boolean
}
class ColorSquareNonUI( color: SColor) : IColorSquareNonUI {
    override val colorBind = Bindable(color)
    override var color: SColor by colorBind
    override var active: Boolean = true
}
interface IColorSquare : IColorSquareNonUI, IComponent

class SwColorSquare
private constructor(
        defaultColor: SColor,
        val imp : SwColorSquareImp)
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
        var context :SwColorSquare? = null

        init {
            this.isOpaque = false
        }

        override fun paintComponent(g: Graphics) {
            val ctx = context?: return
            g.color = ctx.color.jcolor
            g.fillRect(0, 0, width, height)
        }
    }
}