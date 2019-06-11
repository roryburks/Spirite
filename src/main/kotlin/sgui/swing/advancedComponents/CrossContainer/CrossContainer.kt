package sgui.swing.advancedComponents.CrossContainer

import sgui.generic.Orientation
import sgui.generic.components.initializers.CSE_Group
import sgui.generic.components.initializers.CrossInitializer
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