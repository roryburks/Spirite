package spirite.gui.components.basic

import java.awt.Color

interface ILabel : IComponent {
    var label : String
    var textColor : Color

    var bold : Boolean
    var textSize : Int
}