package spirite.gui.Basic

import spirite.gui.Skin
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.border.BevelBorder

class SButton : JButton {
    constructor(str: String? = null) {
        text = str
        background = Skin.Global.BgDark.color
        foreground = Skin.Global.Text.color
        border = BorderFactory.createBevelBorder(
                BevelBorder.RAISED, Skin.BevelBorder.Med.color, Skin.BevelBorder.Dark.color)
    }
}