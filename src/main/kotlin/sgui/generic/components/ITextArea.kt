package sgui.generic.components

import rb.owl.bindable.Bindable

interface ITextArea : IComponent {
    val textBind : Bindable<String>
    var text: String
}