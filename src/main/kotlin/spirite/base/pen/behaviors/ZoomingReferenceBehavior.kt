package spirite.base.pen.behaviors

import rb.vectrix.mathUtil.d
import rb.vectrix.mathUtil.f
import spirite.base.pen.Penner

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