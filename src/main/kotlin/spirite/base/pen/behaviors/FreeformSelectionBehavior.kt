package spirite.base.pen.behaviors

import spirite.base.brains.toolset.BoxSelectionShape
import spirite.base.graphics.GraphicsContext
import spirite.base.imageData.selection.ISelectionEngine.BuildMode
import spirite.base.pen.Penner
import spirite.base.pen.selectionBuilders.FreeformSelectionBuilder
import spirite.base.util.Colors
import spirite.base.util.MUtil
import spirite.base.util.f

class FreeformSelectionBehavior(
        penner: Penner,
        val mode: BuildMode)
    : DrawnPennerBehavior(penner)
{
    val builder = FreeformSelectionBuilder(penner.workspace!!)
    var drawing = true

    override fun onStart() {
        builder.update(penner.x, penner.y)
    }

    override fun onTock() {}

    override fun onMove() {
        if( drawing && (penner.x != penner.oldX || penner.y != penner.oldY))
            builder.update(penner.x, penner.y)
    }

    override fun onPenUp() {
        drawing = false
        testFinish()
    }

    override fun onPenDown() {
        drawing = true
        if( !testFinish())
            builder.update(penner.x, penner.y)
    }

    override fun paintOverlay(gc: GraphicsContext) {
        gc.pushTransform()
        gc.color = Colors.BLACK
        builder.drawBuilding(gc)
    }

    private fun testFinish() : Boolean{
        val start = builder.start
        if( MUtil.distance(start.x.f, start.y.f, penner.x.f, penner.y.f) <= 5) {
            penner.workspace?.selectionEngine?.mergeSelection(builder.build(), mode)
            end()
            return true
        }
        return false
    }
}