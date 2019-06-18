package sgui.components

import rb.glow.color.Color


interface ILabel : IComponent {
    var text : String
    var textColor : Color

    var bold : Boolean
    var textSize : Int
}