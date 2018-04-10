package spirite.base.pen.selectionBuilders

import spirite.base.graphics.GraphicsContext
import spirite.base.imageData.IImageWorkspace
import spirite.base.imageData.selection.Selection
import spirite.base.util.MUtil

class RectSelectionBuilder( workspace: IImageWorkspace) : SelectionBuilder( workspace) {
    var startX: Int = 0
    var startY: Int = 0
    var currentX: Int = 0
    var currentY: Int = 0

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
        return Selection.RectangleSelection(MUtil.rectFromEndpoints(startX, startY, currentX, currentY))
    }

    override fun drawBuilding(gc: GraphicsContext) {
        gc.drawRect(
                Math.min(startX, currentX), Math.min(startY, currentY),
                Math.abs(startX - currentX), Math.abs(startY - currentY))
    }
}