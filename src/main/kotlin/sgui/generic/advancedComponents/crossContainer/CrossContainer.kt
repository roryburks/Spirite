package sgui.generic.advancedComponents.crossContainer

import sgui.generic.Orientation
import sgui.swing.components.SwPanel

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
