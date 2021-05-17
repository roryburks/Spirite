package sgui.core.components

import rb.owl.bindable.Bindable
import sgui.components.IComponent
import sgui.core.IIcon

interface IToggleButtonNonUI {
    val checkBind : Bindable<Boolean>
    var checked : Boolean

}

interface IToggleButton : IToggleButtonNonUI, IComponent {
    var plainStyle : Boolean


    fun setOnIcon( icon: IIcon)
    fun setOffIcon( icon: IIcon)
    fun setOnIconOver( icon: IIcon)
    fun setOffIconOver( icon: IIcon)
}

class ToggleButtonNonUI( startChecked: Boolean = false) : IToggleButtonNonUI {
    override val checkBind = Bindable(startChecked)
    override var checked by checkBind
}
