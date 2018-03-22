package spirite.pc.gui.basic

import spirite.gui.components.basic.IComponent
import spirite.gui.components.basic.IToggleButton
import spirite.gui.components.basic.IToggleButtonNonUI
import spirite.gui.components.basic.ToggleButtonNonUI
import spirite.gui.resources.IIcon
import javax.swing.JToggleButton


class SwToggleButton
private constructor(startChecked: Boolean, private val imp: SwToggleButtonImp )
    : IToggleButton,
        IToggleButtonNonUI by ToggleButtonNonUI(startChecked), IComponent,
        ISwComponent by SwComponent(imp)
{
    override fun setOnIcon(icon: IIcon) {imp.selectedIcon = icon.icon}
    override fun setOffIcon(icon: IIcon) {imp.icon = icon.icon}

    override fun setOnIconOver(icon: IIcon) {imp.rolloverSelectedIcon = icon.icon}
    override fun setOffIconOver(icon: IIcon) {imp.rolloverIcon = icon.icon }

    constructor(startChecked: Boolean = false) : this(startChecked, SwToggleButtonImp())

    override var plainStyle: Boolean = false
        set(value) {
            if( value != field) {
                field = value
                imp.isBorderPainted = !value
                imp.isContentAreaFilled = !value
                imp.isFocusPainted = !value
                imp.isOpaque = !value
            }
        }



    val checkBind = checkBindable.addListener { new, old -> imp.isSelected = new }
    init {
        imp.addItemListener{checked = imp.isSelected}
    }

    private class SwToggleButtonImp : JToggleButton()
}