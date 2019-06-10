package spirite.gui.components.advanced.omniContainer

import sgui.generic.components.IComponent
import spirite.gui.resources.IIcon

interface IOmniComponent {
    val component: IComponent
    val icon : IIcon?
    val name : String

    fun close() {}
}