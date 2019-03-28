package spirite.gui.components.basic

import rb.owl.bindable.Bindable
import spirite.pc.gui.SColor

interface IColorSquareNonUI {
    val colorBind : Bindable<SColor>
    var color : SColor
    var active : Boolean
}
class ColorSquareNonUI( color: SColor) : IColorSquareNonUI {
    override val colorBind = Bindable(color)
    override var color: SColor by colorBind
    override var active: Boolean = true
}
interface IColorSquare : IColorSquareNonUI, IComponent

