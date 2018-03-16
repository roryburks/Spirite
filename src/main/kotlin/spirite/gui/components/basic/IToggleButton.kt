package spirite.gui.components.basic

import spirite.gui.Bindable
import spirite.gui.Bindable.Bound

interface IToggleButtonNonUI {
    val checkBindable : Bindable<Boolean>
    var checked : Boolean

}

interface IToggleButton : IToggleButtonNonUI, IComponent {
    var plainStyle : Boolean
}

class ToggleButtonNonUI( startChecked: Boolean = false) : IToggleButtonNonUI{
    override val checkBindable = Bindable(startChecked)
    override var checked by Bound(checkBindable)
}
