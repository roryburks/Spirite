package spirite.gui.components.advanced

import spirite.base.brains.Bindable
import spirite.hybrid.Hybrid


class RadioButtonCluster<T>(
        val defaultValue : T,
        val values: List<T>)
{
    val valueBind = Bindable(defaultValue, { new, old ->
        val selectedIndex = values.indexOf(new)
        if( selectedIndex != -1) {
            radioButtons.forEachIndexed { index, it ->
                it.check = index == selectedIndex
            }
        }
    })
    var value by valueBind

    val radioButtons = values.map { Hybrid.ui.RadioButton(it.toString(), it == defaultValue)}

    init {
        radioButtons.forEachIndexed { index, it ->
            it.checkBind.addListener { new, old ->
                if( new) {
                    value = values[index]
                }
                else {
                    // ugly but effective.  basically just says "make sure the thing the logic says is being selected,
                    //  (thus overwriting the UI's attempt to change)
                    valueBind.onChange?.invoke(value, value)
                }
            }
        }
    }
}