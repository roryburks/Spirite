package spirite.base.pen.selectionBuilders

import rb.glow.GraphicsContext_old
import rb.glow.IGraphicsContext
import rb.glow.drawer
import rb.vectrix.mathUtil.d
import spirite.base.imageData.IImageWorkspace
import spirite.base.imageData.selection.Selection
import spirite.base.util.linear.RectangleUtil

class RectSelectionBuilder( workspace: IImageWorkspace) : SelectionBuilder( workspace) {
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
        return Selection.RectangleSelection(RectangleUtil.rectFromEndpoints(startX, startY, currentX, currentY))
    }

    override fun drawBuilding(gc: IGraphicsContext) {
        gc.drawer.drawRect(
                Math.min(startX, currentX).d, Math.min(startY, currentY).d,
                Math.abs(startX - currentX).d, Math.abs(startY - currentY).d)
    }
}