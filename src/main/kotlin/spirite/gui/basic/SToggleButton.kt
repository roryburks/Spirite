package spirite.gui.basic

import spirite.gui.Bindable
import spirite.gui.Bindable.Bound
import javax.swing.JComponent
import javax.swing.JToggleButton

interface IToggleButtonNonUI {
    val checkBindable : Bindable<Boolean>
    var checked : Boolean
}

interface IToggleButton : IToggleButtonNonUI, IComponent

class SToggleButtonNonUI( startChecked: Boolean = false) : IToggleButtonNonUI{
    override val checkBindable = Bindable(startChecked)
    override var checked by Bound(checkBindable)
}

class SToggleButton
private constructor(startChecked: Boolean, invokable: Invokable<JComponent> )
    : JToggleButton(), IToggleButton,
        IToggleButtonNonUI by SToggleButtonNonUI(startChecked), IComponent,
        ISComponent by SComponent(invokable)
{
    init {invokable.invoker = {this}}
    constructor(startChecked: Boolean = false) : this(startChecked, Invokable())

    init {
        Bindable(false, {isSelected = it}).bind(checkBindable)
        this.addItemListener {            checked = isSelected}
    }
}
