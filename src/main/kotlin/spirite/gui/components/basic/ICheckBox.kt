package spirite.gui.components.basic

import spirite.base.util.binding.CruddyBindable

interface ICheckBox : IComponent {
    val checkBind : CruddyBindable<Boolean>
    var check : Boolean
}

interface IRadioButton : IComponent {
    val checkBind : CruddyBindable<Boolean>
    var check : Boolean
    var label : String
}
