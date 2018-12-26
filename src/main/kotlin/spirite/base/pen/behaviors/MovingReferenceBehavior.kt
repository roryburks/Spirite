package spirite.base.pen.behaviors

import rb.vectrix.mathUtil.f
import spirite.base.pen.Penner

class MovingReferenceBehavior(penner: Penner) : PennerBehavior(penner) {
    override fun onStart() {}
    override fun onTock() {}
    override fun onMove() {
        penner.workspace?.referenceManager?.shiftTransform( (penner.x - penner.oldX).f, (penner.y - penner.oldY).f)
    }
}