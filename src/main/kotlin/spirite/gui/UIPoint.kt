package spirite.gui

import spirite.gui.components.basic.IComponent
import spirite.pc.gui.basic.ISwComponent
import javax.swing.JComponent
import javax.swing.SwingUtilities

abstract class UIPoint(
        val x : Int,
        val y: Int,
        val component: IComponent)
{
    abstract fun convert( other: IComponent) : UIPoint
}

class SUIPoint( x:Int, y:Int, component: ISwComponent) : UIPoint(x, y, component)
{
    override fun convert(other: IComponent) = when( other) {
        is ISwComponent -> {
            val converted = SwingUtilities.convertPoint( component.component as JComponent, x, y, other.component)
            SUIPoint( converted.x, converted.y, other)
        }
        else -> throw Exception("Tried to mix an SwComponentIndirect with $component")
    }
}