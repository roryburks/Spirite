package spirite.gui.components.advanced.omniContainer

import sgui.components.IComponent
import sgui.swing.SwIcon

interface IOmniComponent {
    val component: IComponent
    val icon : SwIcon?
    val name : String

    fun close() {}
}