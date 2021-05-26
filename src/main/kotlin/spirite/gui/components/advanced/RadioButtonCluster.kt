package spirite.gui.components.advanced

import rb.owl.bindable.Bindable
import rb.owl.bindable.addObserver
import rb.owl.observer
import sgui.hybrid.Hybrid


class RadioButtonCluster<T>(
        val defaultValue : T,
        val values: List<T>)
{
    private val _onChange = { new: T, old: T ->
        val selectedIndex = values.indexOf(new)
        if (selectedIndex != -1) {
            radioButtons.forEachIndexed { index, it ->
                it.check = index == selectedIndex
            }
        }
    }
    val valueBind = Bindable(defaultValue)
            .also { it.addObserver(_onChange.observer(), false) }
    var value by valueBind

    val radioButtons = values.map { Hybrid.ui.RadioButton(it.toString(), it == defaultValue)}

    init {
        radioButtons.forEachIndexed { index, it ->
            it.checkBind.addObserver { new, _ ->
                if( new) {
                    value = values[index]
                }
                else {
                    // ugly but effective.  basically just says "make sure the thing the logic says is being selected,
                    //  (thus overwriting the UI's attempt to change)
                    _onChange.invoke(value, value)
                }
            }
        }
    }
}