package spirite.gui.major.work

import spirite.base.graphics.GraphicsContext
import spirite.base.util.Colors
import spirite.base.util.linear.MutableTransform
import spirite.base.util.linear.Transform
import spirite.base.util.linear.Transform.Companion
import spirite.gui.basic.ISComponent

abstract class WorkArea(
        val context: WorkSection) {
    abstract val scomponent: ISComponent

    fun drawWork( gc: GraphicsContext) {
        gc.clear()
        gc.color = Colors.RED

        val view = context.currentView

        if( view != null) {
            gc.transform = view.tWorkspaceToScreen
            gc.fillRect(0, 0, 200, 200)
        }
    }

}