package spirite.pc.gui.basic

import spirite.base.brains.Bindable
import spirite.gui.components.basic.ICheckBox
import spirite.gui.components.basic.IComponent
import spirite.gui.components.basic.IRadioButton
import javax.swing.JCheckBox
import javax.swing.JRadioButton

class SwCheckBox
private constructor(val imp : SwCheckBoxImp)
    : ICheckBox, IComponent by SwComponent(imp)
{
    constructor() : this(SwCheckBoxImp())

    override val checkBind = Bindable(imp.isSelected)
    override var check by checkBind

    init {
        imp.addItemListener { check = imp.isSelected }
        checkBind.addListener { new, old ->  imp.isSelected = new }
    }

    private class SwCheckBoxImp() : JCheckBox() {}
}

class SwRadioButton
private constructor(val imp : SwRadioButtonImp)
    : IRadioButton, IComponent by SwComponent(imp)
{
    constructor(label: String = "", selected: Boolean = false) : this(SwRadioButtonImp(label, selected))

    override val checkBind = Bindable(imp.isSelected)
    override var check by checkBind
    override var label: String
        get() = imp.text
        set(value) {imp.text = value}

    init {
        imp.addItemListener { check = imp.isSelected }
        checkBind.addListener { new, old ->  imp.isSelected = new }
    }

    private class SwRadioButtonImp(label: String , selected: Boolean) : JRadioButton(label, selected) {}
}