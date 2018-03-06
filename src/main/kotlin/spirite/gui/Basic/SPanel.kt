package spirite.gui.Basic

import spirite.gui.Skin.Global.Bg
import javax.swing.JPanel

open class SPanel : JPanel() {
    init {
        background = Bg.color;
    }
}