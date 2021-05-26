package sgui.swing.components

import rb.owl.bindable.Bindable
import rb.owl.bindable.addObserver
import sgui.core.components.ICheckBox
import sgui.components.IComponent
import sgui.core.components.IRadioButton
import sgui.swing.systems.mouseSystem.adaptMouseSystem
import sgui.swing.skin.Skin
import sguiSwing.components.SwComponent
import javax.swing.JCheckBox
import javax.swing.JRadioButton

class SwCheckBox
private constructor(private val imp : SwCheckBoxImp)
    : ICheckBox, IComponent by SwComponent(imp)
{
    constructor() : this(SwCheckBoxImp())

    override val checkBind = Bindable(imp.isSelected)
    override var check by checkBind

    init {
        imp.addItemListener { check = imp.isSelected }
        checkBind.addObserver { new, _ ->  imp.isSelected = new }
    }

    private class SwCheckBoxImp() : JCheckBox() {
        init {
            adaptMouseSystem()
            background = Skin.Global.Bg.jcolor
        }
    }
}

class SwRadioButton
private constructor(private val imp : SwRadioButtonImp)
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
        checkBind.addObserver { new, _ ->  imp.isSelected = new }
    }

    private class SwRadioButtonImp(label: String , selected: Boolean) : JRadioButton(label, selected) {
        init {
            adaptMouseSystem()
            background = Skin.Global.Bg.jcolor
        }
    }
}