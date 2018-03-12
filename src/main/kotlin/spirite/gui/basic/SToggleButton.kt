package spirite.gui.basic

import spirite.gui.Bindable
import spirite.gui.Bindable.Bound
import javax.swing.JComponent
import javax.swing.JToggleButton

interface IToggleButtonNonUI {
    val checkBindable : Bindable<Boolean>
    var checked : Boolean

}

interface IToggleButton : IToggleButtonNonUI, IComponent {
    var plainStyle : Boolean
}

class SToggleButtonNonUI( startChecked: Boolean = false) : IToggleButtonNonUI{
    override val checkBindable = Bindable(startChecked)
    override var checked by Bound(checkBindable)
}

class SToggleButton
private constructor(startChecked: Boolean, private val imp: SToggleButtonImp )
    : IToggleButton,
        IToggleButtonNonUI by SToggleButtonNonUI(startChecked), IComponent,
        ISComponent by SComponentDirect(imp)
{
    constructor(startChecked: Boolean = false) : this(startChecked, SToggleButtonImp())

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

    private class SToggleButtonImp : JToggleButton()
}
