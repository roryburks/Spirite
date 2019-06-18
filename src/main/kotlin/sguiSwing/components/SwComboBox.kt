package sguiSwing.components

import rb.owl.bindable.addObserver
import sgui.components.ComboBox
import sgui.components.IComponent
import sguiSwing.skin.Skin.Global.Fg
import sguiSwing.skin.Skin.Global.TextDark
import java.awt.event.ActionListener
import javax.swing.JComboBox
import javax.swing.ListCellRenderer

class SwComboBox<T>
private constructor(
        things: Array<T>,
        private val imp: SwComboBoxImp<T>)
    : ComboBox<T>(things.toList()), ISwComponent by SwComponent(imp)
{
    override var renderer: ((value: T?, index: Int, isSelected: Boolean, hasFocus: Boolean) -> IComponent)? = null
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

    constructor(things: Array<T>) : this( things, SwComboBoxImp<T>(things))

    init {
        selectedItemBind.addObserver { _, _ -> imp.selectedIndex = selectedIndex}
        imp.addActionListener(_listener)
    }

    class SwComboBoxImp<T>(things: Array<T>) : JComboBox<T>(things)
    {
        val defaultRenderer = renderer

        init {
            background = Fg.jcolor
            foreground = TextDark.jcolor
        }
    }

}