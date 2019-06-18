package sguiSwing

import sgui.UIPoint
import sgui.components.IComponent
import sguiSwing.components.jcomponent
import java.awt.Component
import javax.swing.SwingUtilities

class SUIPoint( x:Int, y:Int, val component: Component) : UIPoint(x, y)
{
    override fun convert(other: IComponent) : sguiSwing.SUIPoint {
        val converted = SwingUtilities.convertPoint(component, x, y, other.jcomponent)
        return sguiSwing.SUIPoint(converted.x, converted.y, other.jcomponent)
    }
}