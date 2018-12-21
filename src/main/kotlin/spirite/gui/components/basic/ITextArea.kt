package spirite.gui.components.basic

import spirite.base.util.binding.CruddyBindable

interface ITextArea : IComponent{
    val textBind : CruddyBindable<String>
    var text: String
}