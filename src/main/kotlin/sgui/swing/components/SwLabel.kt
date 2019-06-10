package sgui.swing.components

import sgui.generic.components.ILabel
import sgui.skin.Skin
import sgui.swing.adaptMouseSystem
import java.awt.Color
import java.awt.Font
import javax.swing.JLabel


class SwLabel
private constructor( private val imp : SwLabelImp)
    : ILabel,
        ISwComponent by SwComponent(imp)
{
    constructor( text: String = "") : this(SwLabelImp(text))

    override var text: String
        get() = imp.text
        set(value) {imp.text = value}
    override var textColor: Color
        get() = imp.foreground
        set(value) {imp.foreground = value}

    override var bold = true
        set(value) {
            field = value
            imp.font = Font(if( textSize < 10)"Palatino" else "Tahoma", if(bold) Font.BOLD else Font.PLAIN, textSize)
        }
    override var textSize: Int = 16
        set(value) {
            field = value
            imp.font = Font(if( textSize < 10)"Palatino" else "Tahoma", if(bold) Font.BOLD else Font.PLAIN, textSize)
        }

    private class SwLabelImp( text: String) : JLabel(text) {
        init {
            adaptMouseSystem()
            foreground = Skin.Global.Text.jcolor
        }
    }
}