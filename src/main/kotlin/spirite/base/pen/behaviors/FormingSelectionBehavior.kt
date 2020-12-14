package spirite.base.pen.behaviors

import rb.glow.Colors
import rb.glow.IGraphicsContext
import spirite.base.brains.toolset.BoxSelectionShape
import spirite.base.brains.toolset.BoxSelectionShape.OVAL
import spirite.base.brains.toolset.BoxSelectionShape.RECTANGLE
import spirite.base.imageData.selection.ISelectionEngine.BuildMode
import spirite.base.pen.Penner
import spirite.base.pen.selectionBuilders.OvalSelectionBuilder
import spirite.base.pen.selectionBuilders.RectSelectionBuilder
import spirite.base.pen.selectionBuilders.SelectionBuilder
import spirite.gui.views.work.WorkSectionView


class FormingSelectionBehavior(
        penner: Penner,
        shape: BoxSelectionShape,
        val mode: BuildMode)
    : DrawnPennerBehavior(penner)
{
    private val builder : SelectionBuilder = when(shape) {
        RECTANGLE -> RectSelectionBuilder(penner.workspace!!)
        OVAL -> OvalSelectionBuilder(penner.workspace!!)
    }

    override fun onStart() = builder.start(penner.x, penner.y)
    override fun onTock() {}
    override fun onMove() = builder.update(penner.x, penner.y)

    override fun onPenUp() {
        val built = builder.build()

        when(built) {
            null -> penner.workspace?.selectionEngine?.setSelection(null)
            else -> penner.workspace?.selectionEngine?.mergeSelection(built, mode)
        }

        super.onPenUp()
    }

    override fun paintOverlay(gc: IGraphicsContext, view: WorkSectionView) {
        gc.pushTransform()
        gc.color = Colors.BLACK
        builder.drawBuilding(gc)
        gc.popTransform()
    }
}