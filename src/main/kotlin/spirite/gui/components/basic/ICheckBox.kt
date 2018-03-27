package spirite.gui.components.basic

import spirite.base.brains.Bindable

interface ICheckBox : IComponent {
    val checkBind : Bindable<Boolean>
    var check : Boolean
}

interface IRadioButton : IComponent {
    val checkBind : Bindable<Boolean>
    var check : Boolean
    var label : String
}
