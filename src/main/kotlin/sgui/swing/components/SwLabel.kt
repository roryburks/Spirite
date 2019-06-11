package sgui.swing.components

import rb.glow.color.SColor
import sgui.generic.components.ILabel
import sgui.swing.skin.Skin
import sgui.swing.mouseSystem.adaptMouseSystem
import sgui.swing.jcolor
import sgui.swing.scolor
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
    override var textColor: SColor
        get() = imp.foreground.scolor
        set(value) {imp.foreground = value.jcolor}

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