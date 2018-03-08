package spirite.gui.Basic

import spirite.gui.Skin
import java.awt.event.ActionListener
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.border.BevelBorder

interface IButton : IComponent {
    var action: (()->Unit)?
}

class SButton : JButton, IButton {

    constructor(str: String? = null) {
        text = str
        background = Skin.Global.BgDark.color
        foreground = Skin.Global.Text.color
        border = BorderFactory.createBevelBorder(
                BevelBorder.RAISED, Skin.BevelBorder.Med.color, Skin.BevelBorder.Dark.color)
    }

    override var action: (() -> Unit)? = null

    init {
        addActionListener { action?.invoke() }
    }
}