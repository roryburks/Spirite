package spirite.gui.advanced.crossContainer

import spirite.gui.Orientation
import spirite.gui.basic.IComponent
import spirite.gui.basic.SPanel
import javax.swing.JPanel

open class CrossContainer(constructor: CrossInitializer.()->Unit): SPanel()
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
