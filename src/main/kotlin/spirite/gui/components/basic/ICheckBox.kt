package spirite.gui.components.basic

import rb.owl.bindable.Bindable

interface ICheckBox : IComponent {
    val checkBind : Bindable<Boolean>
    var check : Boolean
}

interface IRadioButton : IComponent {
    val checkBind : Bindable<Boolean>
    var check : Boolean
    var label : String
}
