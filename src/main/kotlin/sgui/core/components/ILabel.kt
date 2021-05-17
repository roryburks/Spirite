package sgui.core.components

import rb.glow.Color
import sgui.components.IComponent


interface ILabel : IComponent {
    var text : String
    var textColor : Color

    var bold : Boolean
    var textSize : Int
}