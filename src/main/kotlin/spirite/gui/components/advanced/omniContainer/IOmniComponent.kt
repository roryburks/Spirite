package spirite.gui.components.advanced.omniContainer

import spirite.gui.components.basic.IComponent

interface IOmniComponent {
    val component: IComponent

    fun close() {}
}