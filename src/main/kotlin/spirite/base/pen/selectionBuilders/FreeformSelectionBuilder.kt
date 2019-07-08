package spirite.base.pen.selectionBuilders

import rb.glow.GraphicsContext
import rb.glow.color.Colors
import rb.vectrix.compaction.IntCompactor
import rb.vectrix.linear.Vec2i
import rb.vectrix.mathUtil.f
import spirite.base.imageData.IImageWorkspace
import spirite.base.imageData.selection.Selection
import spirite.hybrid.Hybrid

class FreeformSelectionBuilder( workspace: IImageWorkspace) : SelectionBuilder(workspace) {
    private val xCompactor = IntCompactor()
    private val yCompactor = IntCompactor()

    val start get() = Vec2i(xCompactor[0], yCompactor[0])
    val end get() = Vec2i(xCompactor[xCompactor.size - 1], yCompactor[yCompactor.size - 1])

    override fun start(x: Int, y: Int) {
        xCompactor.add(x)
        yCompactor.add(y)
    }

    override fun update(x: Int, y: Int) {
        xCompactor.add(x)
        yCompactor.add(y)
    }

    override fun build(): Selection {
        // Lifecycle tied to the selection
        val img = Hybrid.imageCreator.createImage(workspace.width, workspace.height)
        val gc = img.graphics
        gc.color = Colors.WHITE
        gc.fillPolygon( xCompactor.toArray().map { it.f },  yCompactor.toArray().map { it.f }, xCompactor.size)
        return Selection(img, null, true)
    }

    override fun drawBuilding(gc: GraphicsContext) {
        for( i in 0 until xCompactor.chunkCount) {
            gc.drawPolyLine( xCompactor.getChunk(i), yCompactor.getChunk(i), xCompactor.getChunkSize(i))
        }
    }
}