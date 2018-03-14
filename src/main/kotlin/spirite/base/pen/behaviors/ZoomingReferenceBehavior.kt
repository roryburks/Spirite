package spirite.base.pen.behaviors

import spirite.base.pen.Penner
import spirite.base.util.d
import spirite.base.util.f

class ZoomingReferenceBehavior(penner: Penner) : PennerBehavior(penner) {

    val startX = penner.x
    val startY = penner.y

    override fun onStart() {}
    override fun onTock() {}
    override fun onMove() {
        penner.workspace?.referenceManager?.zoomTransform(
                Math.pow(1.0015, 1 + (penner.rawY - penner.oldRawY).d).f, startX, startY) ?: end()
    }

}