package sguiSwing.advancedComponents.CrossContainer

import sgui.components.crossContainer.CSE_Group
import sgui.components.crossContainer.CrossInitializer
import sguiSwing.components.SwPanel

open class CrossContainer(constructor: CrossInitializer.()->Unit): SwPanel()
{
    val rootOrientation: sgui.Orientation
    internal val rootGroup : CSE_Group?

    init {
        val scheme= CrossInitializer().apply { constructor.invoke(this) }.scheme
        rootOrientation = scheme.baseOrientation
        rootGroup = scheme.rootGroup

        setLayout( constructor)
    }
}

