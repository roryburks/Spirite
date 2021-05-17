package sgui.core.components

import rb.owl.bindable.Bindable
import sgui.components.IComponent

interface ITextArea : IComponent {
    val textBind : Bindable<String>
    var text: String
}