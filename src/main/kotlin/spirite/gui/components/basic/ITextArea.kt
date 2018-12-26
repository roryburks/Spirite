package spirite.gui.components.basic

import rb.owl.bindable.Bindable

interface ITextArea : IComponent{
    val textBind : Bindable<String>
    var text: String
}