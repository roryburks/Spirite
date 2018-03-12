package spirite.gui.basic

import spirite.gui.Skin
import java.awt.Color
import javax.swing.JComponent
import javax.swing.JLabel

interface ILabel : IComponent {
    var label : String
    var textColor : Color
}

class SLabel
private constructor( val imp : SLabelImp)
    : ILabel,
        ISComponent by SComponentDirect(imp)
{
    constructor( text: String = "") : this(SLabelImp(text))

    override var label: String
        get() = imp.text
        set(value) {imp.text = value}
    override var textColor: Color
        get() = imp.foreground
        set(value) {imp.foreground = value}

    private class SLabelImp( text: String) : JLabel(text) {
        init {
            foreground = Skin.Global.Text.color
        }
    }
}