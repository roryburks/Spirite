package spirite.gui.views.work

import rb.glow.*
import rb.vectrix.mathUtil.d
import sguiSwing.components.ISwComponent
import sguiSwing.hybrid.Hybrid
import sguiSwing.skin.Skin
import spirite.specialRendering.SpecialDrawerFactory
import java.io.File

abstract class WorkArea(
        val context: WorkSection) {
    abstract val scomponent: ISwComponent

    var i = 0

    fun drawWork( gc: IGraphicsContext) {
        gc.clear( Skin.Global.Bg.scolor)
        gc.color = Colors.RED

        val workspace = context.currentWorkspace
        val view = context.currentView

        if( view != null && workspace != null) {
            gc.transform = view.tWorkspaceToScreen

            val drawer = SpecialDrawerFactory.makeSpecialDrawer(gc)
            drawer.drawTransparencyBg(0, 0, workspace.width, workspace.height, 8)
            gc.color = Colors.WHITE

            // TODO: Draw Reference Behind

            val rimg = workspace.renderEngine.renderWorkspace(workspace)
            gc.renderImage(rimg, 0.0, 0.0)
            //Hybrid.imageIO.saveImage(rimg, File("C:\\Bucket\\r.png"))

            // TODO: Draw Reference In Front

            // ::: Border Around Active Data
            val active = workspace.activeData
            if( active != null) {
                gc.pushTransform()

                gc.alpha = 0.3f
                gc.color = Skin.DrawPanel.LayerBorder.scolor
                gc.transform( active.tMediumToWorkspace)
                //gc.drawRect(active.handle.x, active.handle.y, active.handle.width, active.handle.height)

                gc.popTransform()
            }

            // :::: Selection Bounds
            val selection = workspace.selectionEngine.selection
            if( selection != null) {
                gc.pushTransform()
                // Why is this transform instead of preTransform?  Doesn't quite seem right.
                selection.transform?.let { gc.transform(it) }

//                drawer.drawBounds(selection.mask, ++i)
//                workspace.selectionEngine.selectionExtra?.draw(gc)

                gc.popTransform()
            }

//            if( context.penner.drawsOverlay)
//                context.penner.drawOverlay(gc, view)
        }
    }

}