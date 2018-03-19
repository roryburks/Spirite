package spirite.gui.components.basic

import spirite.base.util.MUtil
import spirite.gui.Bindable
import spirite.gui.resources.Skin
import spirite.pc.gui.basic.ISwComponent
import spirite.pc.gui.basic.SwComponent
import javax.swing.JComboBox

interface IComboBox<T> : IComponent
{
    var selectedItem : T?
    val selectedItemBind : Bindable<T?>

    var selectedIndex: Int
    val selectedIndexBind: Bindable<Int>

    val values : List<T>
}

abstract class ComboBox<T>(initialValues: List<T>)  :IComboBox<T>
{
    override val selectedItemBind = Bindable<T?>(null,
            {values.indexOf(it).apply { if( this != selectedIndex) selectedIndex = this }})
    override val selectedIndexBind = Bindable(0, {selectedItemBind.field = values[it]})

    override var selectedItem: T?
        get() = selectedItemBind.field
        set(value) {
            val index = values.indexOf( value)
            selectedIndexBind.field = index
        }
    override var selectedIndex: Int
        get() = selectedIndexBind.field
        set(value) {
            val to = MUtil.clip(-1, value, values.size-1)
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
        selectedIndexBind.addListener { imp.selectedIndex = it }
        imp.addActionListener {selectedIndex = imp.selectedIndex}
    }

    class SwComboBoxImp<T>(things: Array<T>) : JComboBox<T>(things)
    {
        init {
            background = Skin.Global.Fg.color
            foreground = Skin.Global.TextDark.color
        }
    }
}