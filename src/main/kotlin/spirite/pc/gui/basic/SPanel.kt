package spirite.pc.gui.basic

import spirite.gui.resources.Skin.Global.Bg
import javax.swing.JPanel

open class SJPanel : JPanel() {

    init {
        background = Bg.color;
    }
}