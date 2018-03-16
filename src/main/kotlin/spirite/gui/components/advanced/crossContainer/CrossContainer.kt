package spirite.gui.components.advanced.crossContainer

import spirite.gui.Orientation
import spirite.pc.gui.basic.SwPanel

open class CrossContainer(constructor: CrossInitializer.()->Unit): SwPanel()
{
    val rootOrientation: Orientation
    internal val rootGroup : CSE_Group?

    init {
        val scheme= CrossInitializer().apply { constructor.invoke(this) }.scheme
        rootOrientation = scheme.baseOrientation
        rootGroup = scheme.rootGroup

        setLayout( constructor)
    }
}

data class CrossScheme(
        val baseOrientation: Orientation,
        val rootGroup: CSE_Group?)