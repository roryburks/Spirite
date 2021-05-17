package spirite.base.pen.behaviors

import rb.glow.Colors
import rb.glow.IGraphicsContext
import rb.vectrix.mathUtil.MathUtil
import rb.vectrix.mathUtil.f
import sgui.core.components.events.MouseEvent.MouseButton
import spirite.base.imageData.selection.ISelectionEngine.BuildMode
import spirite.base.pen.Penner
import spirite.base.pen.selectionBuilders.FreeformSelectionBuilder
import spirite.gui.views.work.WorkSectionView

class FreeformSelectionBehavior(
        penner: Penner,
        val mode: BuildMode)
    : DrawnPennerBehavior(penner)
{
    private val builder = FreeformSelectionBuilder(penner.workspace!!)
    private var drawing = true

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

    override fun onPenDown(button: MouseButton) {
        drawing = true
        if( !testFinish())
            builder.update(penner.x, penner.y)
    }

    override fun paintOverlay(gc: IGraphicsContext, view: WorkSectionView) {
        gc.pushTransform()
        gc.color = Colors.BLACK
        builder.drawBuilding(gc)
    }

    private fun testFinish() : Boolean{
        val start = builder.start
        if( MathUtil.distance(start.xi.f, start.yi.f, penner.x.f, penner.y.f) <= 5) {
            penner.workspace?.selectionEngine?.mergeSelection(builder.build(), mode)
            end()
            return true
        }
        return false
    }
}