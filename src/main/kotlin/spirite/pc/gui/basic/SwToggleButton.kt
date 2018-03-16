package spirite.pc.gui.basic

import spirite.gui.components.basic.IComponent
import spirite.gui.components.basic.IToggleButton
import spirite.gui.components.basic.IToggleButtonNonUI
import spirite.gui.components.basic.ToggleButtonNonUI
import javax.swing.JToggleButton


class SwToggleButton
private constructor(startChecked: Boolean, private val imp: SwToggleButtonImp )
    : IToggleButton,
        IToggleButtonNonUI by ToggleButtonNonUI(startChecked), IComponent,
        ISwComponent by SwComponent(imp)
{
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



    init {
        checkBindable.addListener { imp.isSelected = it }
        imp.addItemListener{checked = imp.isSelected}
    }

    private class SwToggleButtonImp : JToggleButton()
}