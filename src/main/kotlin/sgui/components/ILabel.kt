package sgui.components

import rb.glow.Color


interface ILabel : IComponent {
    var text : String
    var textColor : Color

    var bold : Boolean
    var textSize : Int
}