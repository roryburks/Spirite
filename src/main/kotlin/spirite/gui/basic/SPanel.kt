package spirite.gui.basic

import spirite.gui.Skin.Global.Bg
import javax.swing.JPanel

open class SPanel : JPanel(), IComponent {
    init {
        background = Bg.color;
    }
}