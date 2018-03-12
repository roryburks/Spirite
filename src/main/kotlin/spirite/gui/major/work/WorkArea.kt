package spirite.gui.major.work

import spirite.base.graphics.GraphicsContext
import spirite.base.util.Colors
import spirite.gui.basic.ISComponent

abstract class WorkArea(
        val context: WorkSection) {
    abstract val scomponent: ISComponent

    fun drawWork( gc: GraphicsContext) {
        gc.clear()
        gc.color = Colors.RED
        gc.preTranslate( context.view.offsetX + 0f, context.view.offsetY + 0f)
        gc.fillRect( 10, 10, 20, 20)
    }

}