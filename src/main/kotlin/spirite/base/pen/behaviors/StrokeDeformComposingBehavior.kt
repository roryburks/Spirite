package spirite.base.pen.behaviors

import spirite.base.graphics.GraphicsContext
import spirite.base.imageData.drawer.IImageDrawer.IDeformDrawer
import spirite.base.imageData.selection.ISelectionEngine.BuildMode
import spirite.base.pen.Penner
import spirite.gui.views.work.WorkSectionView


class StrokeDeformComposingBehavior(
        penner: Penner,
        val drawer : IDeformDrawer)
    : DrawnPennerBehavior(penner)
{
    override fun onStart() {
    }

    override fun onEnd() {
    }

    override fun onTock() {
    }

    override fun onMove() {
    }

    override fun paintOverlay(gc: GraphicsContext, view: WorkSectionView) {
    }

    override fun onPenDown() {
    }

    override fun onPenUp() {
    }

}