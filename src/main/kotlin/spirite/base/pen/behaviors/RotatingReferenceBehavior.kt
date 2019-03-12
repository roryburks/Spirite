package spirite.base.pen.behaviors

import spirite.base.pen.Penner

class RotatingReferenceBehavior(penner: Penner) : PennerBehavior(penner)
{
    private val startX = penner.x
    private val startY = penner.y

    override fun onStart() {}
    override fun onTock() {}

    override fun onMove() {
        val theta = (penner.rawX - penner.oldRawX)* 0.05f
        penner.workspace?.referenceManager?.rotateTransform( theta, startX, startY) ?: end()
    }

}