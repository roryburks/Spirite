package spirite.gui.Basic

import spirite.base.util.DataBinding
import spirite.gui.Bindable
import spirite.gui.Bindable.Bound
import javax.swing.JToggleButton

class SToggleButton(startChecked: Boolean = false) : JToggleButton() {
    val checkBindable = Bindable(startChecked, {isSelected = it})
    var checked by Bound(checkBindable)

    private val checkedDB = DataBinding<Boolean>()

    init {
        isSelected = startChecked
        this.addItemListener {
            println(checked)
            checked = isSelected}
    }
}
