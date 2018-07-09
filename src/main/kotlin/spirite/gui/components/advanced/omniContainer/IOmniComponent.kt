package spirite.gui.components.advanced.omniContainer

import spirite.gui.components.basic.IComponent
import spirite.gui.resources.IIcon

interface IOmniComponent {
    val component: IComponent
    val icon : IIcon?
    val name : String

    fun close() {}
}