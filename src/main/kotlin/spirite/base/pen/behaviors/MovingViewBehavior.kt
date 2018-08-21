package spirite.base.pen.behaviors

import spirite.base.pen.Penner
import spirite.gui.views.work.WorkSectionView


class MovingViewBehavior(
        penner: Penner,
        val view : WorkSectionView)
    : PennerBehavior(penner)
{
    override fun onStart() {}
    override fun onTock() {}

    override fun onMove() {
        view.offsetX -= penner.rawX - penner.oldRawX
        view.offsetY -= penner.rawY - penner.oldRawY
    }
}