package spirite.base.pen.behaviors

import rb.vectrix.linear.ImmutableTransformF
import rb.vectrix.mathUtil.f
import spirite.base.pen.Penner


class MovingSelectionBehavior(penner: Penner) : PennerBehavior(penner)
{
    override fun onStart() {}
    override fun onTock() {}
    override fun onMove() {
        if( penner.oldX != penner.x || penner.oldY != penner.y) {
            penner.workspace?.selectionEngine?.transformSelection(
                    ImmutableTransformF.Translation(penner.x - penner.oldX.f, penner.y - penner.oldY.f), true)
        }
    }
}