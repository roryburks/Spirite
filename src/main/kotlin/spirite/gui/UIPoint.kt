package spirite.gui

import spirite.gui.basic.IComponent
import spirite.gui.basic.ISComponent
import spirite.gui.basic.SComponent
import javax.swing.SwingUtilities

abstract class UIPoint(
        val x : Int,
        val y: Int,
        val component: IComponent)
{
    abstract fun convert( other: IComponent) : UIPoint
}

class SUIPoint( x:Int, y:Int, component: ISComponent) : UIPoint(x, y, component)
{
    override fun convert(other: IComponent) = when( other) {
        is ISComponent -> {
            val converted = SwingUtilities.convertPoint( (component as ISComponent).component, x, y, other.component)
            SUIPoint( converted.x, converted.y, other)
        }
        else -> throw Exception("Tried to mix an SComponent with $component")
    }
}