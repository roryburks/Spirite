package spirite.gui.components.basic

import spirite.base.brains.Bindable

interface ITextArea : IComponent{
    val textBind : Bindable<String>
    var text: String
}