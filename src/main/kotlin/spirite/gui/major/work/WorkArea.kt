package spirite.gui.major.work

import spirite.base.graphics.GraphicsContext
import spirite.base.graphics.rendering.RenderTarget
import spirite.base.graphics.rendering.sources.GroupNodeSource
import spirite.base.util.ColorARGB32
import spirite.base.util.ColorARGB32Normal
import spirite.base.util.Colors
import spirite.base.util.linear.MutableTransform
import spirite.base.util.linear.Rect
import spirite.base.util.linear.Transform
import spirite.base.util.linear.Transform.Companion
import spirite.gui.Skin
import spirite.gui.basic.ISComponent

abstract class WorkArea(
        val context: WorkSection) {
    abstract val scomponent: ISComponent

    fun drawWork( gc: GraphicsContext) {
        gc.clear( Skin.Global.Bg.scolor)
        gc.color = Colors.RED

        val workspace = context.currentWorkspace
        val view = context.currentView

        if( view != null && workspace != null) {
            gc.transform = view.tWorkspaceToScreen
            gc.drawTransparencyBG(0, 0, 200, 200, 8)

            val img =
            workspace.renderEngine.renderImage(RenderTarget(GroupNodeSource(workspace.groupTree.root, workspace)))
            gc.renderImage(img, 0, 0)

            gc.fillRect( 10,10,20,20)
            gc.fillRect( 60,20,20,20)
            gc.fillRect( 20,70,20,20)
            gc.fillRect( 40,40,20,20)
            gc.fillRect( 70,60,20,20)
            //gc.fillRect(0, 0, 200, 200)
        }
    }

}