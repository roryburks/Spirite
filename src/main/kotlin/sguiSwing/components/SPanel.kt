package sguiSwing.components

import sgui.swing.mouseSystem.adaptMouseSystem
import sgui.swing.skin.Skin.Global.Bg
import javax.swing.JPanel

open class SJPanel : JPanel() {
    init {
        adaptMouseSystem()
        background = Bg.jcolor;
    }
}