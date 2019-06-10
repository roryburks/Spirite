package sgui.generic

import sgui.generic.components.IComponent
import sgui.swing.components.jcomponent
import java.awt.Component
import javax.swing.SwingUtilities

abstract class UIPoint(
        val x : Int,
        val y: Int)
{
    abstract fun convert( other: IComponent) : UIPoint
}

class SUIPoint( x:Int, y:Int, val component: Component) : UIPoint(x, y)
{
    override fun convert(other: IComponent) : SUIPoint {
        val converted = SwingUtilities.convertPoint( component, x, y, other.jcomponent)
        return SUIPoint(converted.x, converted.y, other.jcomponent)
    }
}