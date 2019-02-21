package spirite.pc.gui.basic

import spirite.gui.components.basic.IButton
import spirite.gui.components.basic.IButton.ButtonActionEvent
import spirite.gui.resources.IIcon
import spirite.gui.resources.Skin
import spirite.pc.gui.adaptMouseSystem
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.border.BevelBorder


class SwButton
private constructor( val imp: SwButtonImp)
    : IButton, ISwComponent by SwComponent(imp)
{

    constructor(str: String? = null) : this(SwButtonImp(str))

    override fun setIcon(icon: IIcon) {imp.icon = icon.icon}

    override var action: ((ButtonActionEvent) -> Unit)?
        get() = imp.action
        set(value) { imp.action = value}

    private class SwButtonImp( str: String? = null) : JButton() {
        init { adaptMouseSystem()}
        var action: ((ButtonActionEvent) -> Unit)? = null

        init {
            mouseListeners.forEach { removeMouseListener(it)}

            addMouseListener(object : MouseListener{
                override fun mouseReleased(e: MouseEvent) {
                    val evt = ButtonActionEvent(e.isShiftDown, e.isAltDown, e.isControlDown)
                    if( isEnabled) action?.invoke(evt)
                }
                override fun mouseEntered(e: MouseEvent?) {}
                override fun mouseClicked(e: MouseEvent?) {}
                override fun mouseExited(e: MouseEvent?) {}
                override fun mousePressed(e: MouseEvent?) {}
            })

            text = str
            background = Skin.Global.BgDark.jcolor
            foreground = Skin.Global.Text.jcolor
            border = BorderFactory.createBevelBorder(
                    BevelBorder.RAISED, Skin.BevelBorder.Med.jcolor, Skin.BevelBorder.Dark.jcolor)
        }
    }
}