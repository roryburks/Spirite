package sgui.generic.components

import rb.owl.bindable.Bindable
import rb.glow.color.SColor

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

