package spirite.gui.basic

import spirite.gui.Skin
import java.awt.Color
import javax.swing.JLabel

interface ILabel {
    var label : String
    var textColor : Color
}

class SLabel( text : String = "") : JLabel(text), ILabel, IComponent{
    override var label: String
        get() = text
        set(value) {text = value}
    override var textColor: Color
        get() = foreground
        set(value) {foreground = value}

    init {
        foreground = Skin.Global.Text.color
    }
}