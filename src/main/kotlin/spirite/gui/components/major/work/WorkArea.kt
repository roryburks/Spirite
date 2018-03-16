package spirite.gui.components.major.work

import spirite.base.graphics.GraphicsContext
import spirite.base.util.Colors
import spirite.gui.Skin
import spirite.pc.gui.basic.ISwComponent

abstract class WorkArea(
        val context: WorkSection) {
    abstract val scomponent: ISwComponent

    fun drawWork( gc: GraphicsContext) {
        gc.clear( Skin.Global.Bg.scolor)
        gc.color = Colors.RED

        val workspace = context.currentWorkspace
        val view = context.currentView

        if( view != null && workspace != null) {
            gc.transform = view.tWorkspaceToScreen
            gc.drawTransparencyBG(0, 0, 200, 200, 8)

            val img = workspace.renderEngine.renderWorkspace(workspace)
            gc.renderImage(img, 0, 0)
            //gc.fillRect(0, 0, 200, 200)
        }
    }

}