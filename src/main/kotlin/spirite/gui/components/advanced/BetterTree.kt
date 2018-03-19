package spirite.gui.components.advanced

import spirite.base.util.delegates.OnChangeDelegate
import spirite.base.util.linear.Vec2i
import spirite.gui.BindList
import spirite.gui.Bindable
import spirite.gui.components.basic.IComponent
import spirite.gui.components.basic.ICrossPanel
import spirite.hybrid.Hybrid
import spirite.pc.gui.basic.SwPanel
import java.awt.datatransfer.Transferable

class SwTreeView<T>
{
    //fun nodeAtPoint( p: Vec2i)

    interface TreeNodeAttributes<T> {
        val leftComponentBuilder : ((T) -> IComponent?)?
        val componentBuilder : (T) -> IComponent
        fun canImport( trans: Transferable) : Boolean
        //fun import
    }

}