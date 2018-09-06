package spirite.gui.components.basic

import spirite.base.util.binding.Bindable

interface ITextArea : IComponent{
    val textBind : Bindable<String>
    var text: String
}