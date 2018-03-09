package spirite.gui.basic

import spirite.gui.Skin
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.border.BevelBorder

interface IButton : IComponent {
    var action: (()->Unit)?
}

class SButton
private constructor( val invokable: Invokable<JComponent>)
    : JButton(), IButton, ISComponent by SComponent(invokable)
{
    init {invokable.invoker = {this}}

    constructor(str: String? = null) : this(Invokable()) {
        text = str
        background = Skin.Global.BgDark.color
        foreground = Skin.Global.Text.color
        border = BorderFactory.createBevelBorder(
                BevelBorder.RAISED, Skin.BevelBorder.Med.color, Skin.BevelBorder.Dark.color)
    }

    override var action: (() -> Unit)? = null

    init {
        addActionListener { action?.invoke() }
    }
}