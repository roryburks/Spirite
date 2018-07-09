package spirite.gui

import spirite.gui.components.basic.IComponent
import spirite.pc.gui.basic.jcomponent
import javax.swing.JComponent
import javax.swing.SwingUtilities

abstract class UIPoint(
        val x : Int,
        val y: Int,
        val component: IComponent)
{
    abstract fun convert( other: IComponent) : UIPoint
}

class SUIPoint( x:Int, y:Int, component: IComponent) : UIPoint(x, y, component)
{
    override fun convert(other: IComponent) : SUIPoint {
        val converted = SwingUtilities.convertPoint( component.jcomponent, x, y, other.jcomponent)
        return  SUIPoint( converted.x, converted.y, other)
    }
}