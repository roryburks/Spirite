package spirite.gui.components.basic

import spirite.base.brains.Bindable
import spirite.base.util.MathUtil
import spirite.gui.resources.Skin
import spirite.pc.gui.basic.ISwComponent
import spirite.pc.gui.basic.SwComponent
import javax.swing.JComboBox

interface IComboBox<T> : IComponent
{
    var selectedItem : T
    val selectedItemBind : Bindable<T>

    var selectedIndex: Int
    val selectedIndexBind: Bindable<Int>

    val values : List<T>
}

abstract class ComboBox<T>(initialValues: List<T>)  :IComboBox<T>
{
    override val selectedItemBind = Bindable<T>(initialValues.first(),
            { new, old -> values.indexOf(new).apply { if (this != selectedIndex) selectedIndex = this } })
    override val selectedIndexBind = Bindable(0, { new, old -> selectedItemBind.field = values[new] })

    override var selectedItem: T
        get() = selectedItemBind.field
        set(value) {
            val index = values.indexOf( value)
            selectedIndexBind.field = index
        }
    override var selectedIndex: Int
        get() = selectedIndexBind.field
        set(value) {
            val to = MathUtil.clip(0, value, values.size-1)
            selectedIndexBind.field = to
        }

    override val values : List<T> get() = _values
    val _values = initialValues.toMutableList()
}

class SwComboBox<T>
private constructor(
        things: Array<T>,
        private val imp: SwComboBoxImp<T>)
    : ComboBox<T>(things.toList()), ISwComponent by SwComponent(imp)
{
    constructor(things: Array<T>) : this( things, SwComboBoxImp<T>( things))

    init {
        selectedIndexBind.addListener { new, old -> imp.selectedIndex = new }
        imp.addActionListener {selectedIndex = imp.selectedIndex}
    }

    class SwComboBoxImp<T>(things: Array<T>) : JComboBox<T>(things)
    {
        init {
            background = Skin.Global.Fg.jcolor
            foreground = Skin.Global.TextDark.jcolor
        }
    }
}