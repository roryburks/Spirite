package spirite.gui.basic

import spirite.gui.Skin.Global.Bg
import javax.swing.JComponent
import javax.swing.JPanel

open class SPanel
private constructor(invokable: Invokable<JComponent>)
    : JPanel(), IComponent,
        ISComponent by SComponent(invokable)
{
    init {invokable.invoker = {this}}
    constructor() : this(Invokable())

    init {
        background = Bg.color;
    }
}