package spirite.base.pen.behaviors

import spirite.base.pen.Penner
import spirite.base.util.f

class GlobalReferenceMoveBehavior( penner: Penner) : PennerBehavior(penner) {
    override fun onStart() {}
    override fun onTock() {}
    override fun onMove() {
        penner.workspace?.referenceManager?.shiftTransform( (penner.x - penner.oldX).f, (penner.y - penner.oldY).f)
    }
}