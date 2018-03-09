package spirite.gui.basic

import spirite.gui.Bindable
import spirite.gui.Bindable.Bound
import javax.swing.JToggleButton

interface IToggleButtonNonUI {
    val checkBindable : Bindable<Boolean>
    var checked : Boolean
}

class SToggleButtonNonUI( startChecked: Boolean = false) : IToggleButtonNonUI{
    override val checkBindable = Bindable(startChecked)
    override var checked by Bound(checkBindable)
}

class SToggleButton(startChecked: Boolean = false)
    : JToggleButton(), IToggleButtonNonUI by SToggleButtonNonUI(startChecked), IComponent
{
    init {
        Bindable(false, {isSelected = it}).bind(checkBindable)
        this.addItemListener {            checked = isSelected}
    }
}
