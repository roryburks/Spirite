package spirite.gui.components.basic

import spirite.base.brains.Bindable
import spirite.base.util.MathUtil
import spirite.gui.resources.Skin
import spirite.pc.gui.basic.ISwComponent
import spirite.pc.gui.basic.SwComponent
import spirite.pc.gui.basic.jcomponent
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import javax.swing.JComboBox
import javax.swing.JList
import javax.swing.ListCellRenderer

interface IComboBox<T> : IComponent
{
    var selectedItem : T?
    val selectedItemBind : Bindable<T?>

    var selectedIndex: Int

    val values : List<T>
    fun setValues( newValues: List<T>, select: T? = null)

    var renderer :((value: T, index: Int, isSelected: Boolean, hasFocus: Boolean) -> IComponent)?
}

abstract class ComboBox<T>(initialValues: List<T>)  :IComboBox<T>
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

class SwComboBox<T>
private constructor(
        things: Array<T>,
        private val imp: SwComboBoxImp<T>)
    : ComboBox<T>(things.toList()), ISwComponent by SwComponent(imp)
{
    override var renderer: ((value: T, index: Int, isSelected: Boolean, hasFocus: Boolean) -> IComponent)? = null
        set(value) {
            if( field != value) {
                field = value
                when(value) {
                    null -> {imp.renderer = imp.defaultRenderer}
                    else -> imp.renderer = ListCellRenderer { list, _value, index, isSelected, cellHasFocus ->
                        value(_value, index, isSelected, cellHasFocus).jcomponent
                    }
                }
            }
        }

    private val _listener = ActionListener { selectedIndex = imp.selectedIndex }

    override fun setValues(newValues: List<T>, select: T?) {
        // Easiest way to prevent Swing-side listens and Spirite-side listeners from conflicting with each other
        // is to just disable the swing-side ones for the duration of the transition so that hear the chatter of Swing
        // messages auto-selecting as the combo box gets de-populated and re-populated.
        imp.removeActionListener(_listener)
        imp.removeAllItems()
        newValues.forEach { imp.addItem(it) }

        _values = newValues.toList()
        selectedItem = select
        imp.selectedIndex = newValues.indexOf(selectedItem)
        imp.addActionListener(_listener)
    }

    constructor(things: Array<T>) : this( things, SwComboBoxImp<T>( things))

    init {
        selectedItemBind.addListener { new, old ->  imp.selectedIndex = selectedIndex}
        imp.addActionListener(_listener)
    }

    class SwComboBoxImp<T>(things: Array<T>) : JComboBox<T>(things)
    {
        val defaultRenderer = renderer

        init {
            background = Skin.Global.Fg.jcolor
            foreground = Skin.Global.TextDark.jcolor
        }
    }

}