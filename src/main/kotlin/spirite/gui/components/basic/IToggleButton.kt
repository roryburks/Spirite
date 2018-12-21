package spirite.gui.components.basic

import spirite.base.util.binding.CruddyBindable
import spirite.gui.resources.IIcon

interface IToggleButtonNonUI {
    val checkBind : CruddyBindable<Boolean>
    var checked : Boolean

}

interface IToggleButton : IToggleButtonNonUI, IComponent {
    var plainStyle : Boolean


    fun setOnIcon( icon: IIcon)
    fun setOffIcon( icon: IIcon)
    fun setOnIconOver( icon: IIcon)
    fun setOffIconOver( icon: IIcon)
}

class ToggleButtonNonUI( startChecked: Boolean = false) : IToggleButtonNonUI{
    override val checkBind = CruddyBindable(startChecked)
    override var checked by checkBind
}
