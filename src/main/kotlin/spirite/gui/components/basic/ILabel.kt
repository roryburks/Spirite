package spirite.gui.components.basic

import java.awt.Color

interface ILabel : IComponent {
    var text : String
    var textColor : Color

    var bold : Boolean
    var textSize : Int
}