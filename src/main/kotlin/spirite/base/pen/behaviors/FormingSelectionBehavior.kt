package spirite.base.pen.behaviors

import spirite.base.brains.toolset.BoxSelectionShape
import spirite.base.brains.toolset.BoxSelectionShape.OVAL
import spirite.base.brains.toolset.BoxSelectionShape.RECTANGLE
import spirite.base.graphics.GraphicsContext
import spirite.base.imageData.groupTree.GroupTree.Node
import spirite.base.imageData.selection.ISelectionEngine.BuildMode
import spirite.base.pen.Penner
import spirite.base.pen.selectionBuilders.RectSelectionBuilder
import spirite.base.pen.selectionBuilders.SelectionBuilder
import spirite.base.util.Colors


class FormingSelectionBehavior(
        penner: Penner,
        shape: BoxSelectionShape,
        val mode: BuildMode)
    : DrawnPennerBehavior(penner)
{

    val builder : SelectionBuilder = when(shape) {
        RECTANGLE -> RectSelectionBuilder(penner.workspace!!)
        OVAL -> TODO()
    }

    override fun onStart() {
        builder.start(penner.x, penner.y)
    }

    override fun onTock() {}

    override fun onMove() {
        builder.update(penner.x, penner.y)
    }

    override fun onPenUp() {
        penner.workspace?.selectionEngine?.mergeSelection(builder.build(), mode)
        super.onPenUp()
    }

    override fun paintOverlay(gc: GraphicsContext) {
        gc.pushTransform()
        gc.color = Colors.BLACK
        builder.drawBuilding(gc)
        gc.popTransform()
    }
}