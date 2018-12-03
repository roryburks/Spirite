package spirite.base.pen.behaviors

import spirite.base.pen.Penner
import rb.vectrix.mathUtil.f
import spirite.base.util.linear.Transform


class MovingSelectionBehavior(penner: Penner) : PennerBehavior(penner)
{
    override fun onStart() {}
    override fun onTock() {}
    override fun onMove() {
        if( penner.oldX != penner.x || penner.oldY != penner.y) {
            penner.workspace?.selectionEngine?.transformSelection(
                    Transform.TranslationMatrix(penner.x - penner.oldX.f, penner.y - penner.oldY.f), true)
        }
    }
}