package spirite.base.pen.selectionBuilders

import rb.glow.IGraphicsContext
import rb.glow.drawer
import rb.vectrix.mathUtil.d
import sgui.core.systems.IImageCreator
import spirite.base.imageData.IImageWorkspace
import spirite.base.imageData.selection.Selection
import spirite.core.hybrid.DiSet_Hybrid
import spirite.sguiHybrid.Hybrid


class OvalSelectionBuilder(
    workspace: IImageWorkspace,
    private val _imageCreator: IImageCreator = DiSet_Hybrid.imageCreator) : SelectionBuilder( workspace)
{
    private var startX: Int = 0
    private var startY: Int = 0
    private var currentX: Int = 0
    private var currentY: Int = 0

    override fun start(x: Int, y: Int) {
        startX = x
        startY = y
        currentX = x
        currentY = y
    }

    override fun update(x: Int, y: Int) {
        currentX = x
        currentY = y
    }

    override fun build(): Selection {
        // Lifecycle tied to the selection
        val img = _imageCreator.createImage(workspace.width, workspace.height)
        img.graphics.drawer.fillOval(
                Math.min(startX, currentX).d, Math.min(startY, currentY).d,
                Math.abs(startX - currentX).d, Math.abs(startY - currentY).d)
        return Selection(img, null, true)
    }

    override fun drawBuilding(gc: IGraphicsContext) {
        gc.drawer.drawOval(
                Math.min(startX, currentX).d, Math.min(startY, currentY).d,
                Math.abs(startX - currentX).d, Math.abs(startY - currentY).d)
    }
}