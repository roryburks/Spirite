package sgui.core.components

import rb.owl.bindable.Bindable
import sgui.components.IComponent

interface ICheckBox : IComponent {
    val checkBind : Bindable<Boolean>
    var check : Boolean
}

interface IRadioButton : IComponent {
    val checkBind : Bindable<Boolean>
    var check : Boolean
    var label : String
}
