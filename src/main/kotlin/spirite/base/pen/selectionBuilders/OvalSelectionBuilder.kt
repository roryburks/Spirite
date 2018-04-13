package spirite.base.pen.selectionBuilders

import spirite.base.graphics.GraphicsContext
import spirite.base.imageData.IImageWorkspace
import spirite.base.imageData.selection.Selection
import spirite.base.util.MUtil
import spirite.hybrid.Hybrid


class OvalSelectionBuilder( workspace: IImageWorkspace) : SelectionBuilder( workspace) {
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
        val img = Hybrid.imageCreator.createImage(workspace.width, workspace.height)
        img.graphics.fillOval(
                Math.min(startX, currentX), Math.min(startY, currentY),
                Math.abs(startX - currentX), Math.abs(startY - currentY))
        return Selection(img, null, true)
    }

    override fun drawBuilding(gc: GraphicsContext) {
        gc.fillOval(
                Math.min(startX, currentX), Math.min(startY, currentY),
                Math.abs(startX - currentX), Math.abs(startY - currentY))
    }
}