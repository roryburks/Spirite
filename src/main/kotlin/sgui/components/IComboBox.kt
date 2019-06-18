package sgui.components

import rb.owl.bindable.Bindable
import rb.vectrix.mathUtil.MathUtil

interface IComboBox<T> : IComponent
{
    var selectedItem : T?
    val selectedItemBind : Bindable<T?>

    var selectedIndex: Int

    val values : List<T>
    fun setValues( newValues: List<T>, select: T? = null)

    var renderer :((value: T?, index: Int, isSelected: Boolean, hasFocus: Boolean) -> IComponent)?
}

abstract class ComboBox<T>(initialValues: List<T>)  : IComboBox<T>
{
    override val selectedItemBind = Bindable<T?>(initialValues.firstOrNull())

    override var selectedItem: T?
        get() = selectedItemBind.field
        set(value) {
            val index = values.indexOf( value)
            selectedItemBind.field = value
        }
    override var selectedIndex: Int
        get() = values.indexOf(selectedItem)
        set(value) {
            val to = MathUtil.clip(0, value, values.size-1)
            selectedItem = values.getOrNull(to)
        }

    override val values : List<T> get() = _values
    protected var _values = initialValues.toList()
}

