package sgui.swing.components

import sgui.swing.skin.Skin.Global.Bg
import sgui.swing.systems.mouseSystem.adaptMouseSystem
import javax.swing.JPanel

open class SJPanel : JPanel() {
    init {
        adaptMouseSystem()
        background = Bg.jcolor;
    }
}