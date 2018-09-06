package spirite.gui.components.basic

import spirite.base.util.binding.Bindable

interface ICheckBox : IComponent {
    val checkBind : Bindable<Boolean>
    var check : Boolean
}

interface IRadioButton : IComponent {
    val checkBind : Bindable<Boolean>
    var check : Boolean
    var label : String
}
