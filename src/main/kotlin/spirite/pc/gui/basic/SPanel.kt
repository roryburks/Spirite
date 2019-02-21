package spirite.pc.gui.basic

import spirite.gui.resources.Skin.Global.Bg
import spirite.pc.gui.adaptMouseSystem
import javax.swing.JPanel

open class SJPanel : JPanel() {

    init {
        adaptMouseSystem()
        background = Bg.jcolor;
    }
}