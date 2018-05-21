package spirite.pc.gui.basic

import spirite.gui.resources.Skin
import spirite.gui.components.basic.ILabel
import java.awt.Color
import java.awt.Font
import javax.swing.JLabel


class SwLabel
private constructor( val imp : SwLabelImp)
    : ILabel,
        ISwComponent by SwComponent(imp)
{
    constructor( text: String = "") : this(SwLabelImp(text))

    override var label: String
        get() = imp.text
        set(value) {imp.text = value}
    override var textColor: Color
        get() = imp.foreground
        set(value) {imp.foreground = value}

    override var bold = true
        set(value) {
            field = value
            imp.font = Font("Tahoma", if(bold) Font.BOLD else Font.PLAIN, textSize)
        }
    override var textSize: Int = 16
        set(value) {
            field = value
            imp.font = Font("Tahoma", if(bold) Font.BOLD else Font.PLAIN, textSize)
        }

    private class SwLabelImp( text: String) : JLabel(text) {
        init {
            foreground = Skin.Global.Text.color
        }
    }
}