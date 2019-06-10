package sgui.swing.components

import sgui.skin.Skin.Global.Bg
import sgui.swing.adaptMouseSystem
import javax.swing.JPanel

open class SJPanel : JPanel() {

    init {
        adaptMouseSystem()
        background = Bg.jcolor;
    }
}