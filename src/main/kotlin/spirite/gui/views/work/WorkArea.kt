package spirite.gui.views.work

import rb.glow.GraphicsContext
import rb.glow.Colors
import sguiSwing.components.ISwComponent
import sguiSwing.skin.Skin
import spirite.specialRendering.SpecialDrawerFactory

abstract class WorkArea(
        val context: WorkSection) {
    abstract val scomponent: ISwComponent

    var i = 0

    fun drawWork( gc: GraphicsContext) {
        gc.clear( Skin.Global.Bg.scolor)
        gc.color = Colors.RED

        val workspace = context.currentWorkspace
        val view = context.currentView

        if( view != null && workspace != null) {
            gc.transform = view.tWorkspaceToScreen

            val drawer = SpecialDrawerFactory.makeSpecialDrawer(gc)
            drawer.drawTransparencyBg(0, 0, workspace.width, workspace.height, 8)

            // TODO: Draw Reference Behind

            gc.renderImage(workspace.renderEngine.renderWorkspace(workspace), 0, 0)

            // TODO: Draw Reference In Front

            // ::: Border Around Active Data
            val active = workspace.activeData
            if( active != null) {
                gc.pushTransform()

                gc.alpha = 0.3f
                gc.color = Skin.DrawPanel.LayerBorder.scolor
                gc.transform( active.tMediumToWorkspace)
                gc.drawRect(active.handle.x, active.handle.y, active.handle.width, active.handle.height)

                gc.popTransform()
            }

            // :::: Selection Bounds
            val selection = workspace.selectionEngine.selection
            if( selection != null) {
                gc.pushTransform()
                // Why is this transform instead of preTransform?  Doesn't quite seem right.
                selection.transform?.let { gc.transform(it) }

                drawer.drawBounds(selection.mask, ++i)
                workspace.selectionEngine.selectionExtra?.draw(gc)

                gc.popTransform()
            }

            if( context.penner.drawsOverlay)
                context.penner.drawOverlay(gc, view)
        }
    }

}