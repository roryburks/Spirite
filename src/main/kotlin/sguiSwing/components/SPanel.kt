package sguiSwing.components

import sguiSwing.mouseSystem.adaptMouseSystem
import sguiSwing.skin.Skin.Global.Bg
import javax.swing.JPanel

open class SJPanel : JPanel() {
    init {
        adaptMouseSystem()
        background = Bg.jcolor;
    }
}