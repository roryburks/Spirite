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
private constructor( text : String, invokable: Invokable<JComponent>)
    : JLabel(text), ILabel,
        ISComponent by SComponent(invokable)
{
    init {invokable.invoker = {this}}
    constructor( text: String = "") : this(text, Invokable())

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