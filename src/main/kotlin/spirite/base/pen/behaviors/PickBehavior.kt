package spirite.base.pen.behaviors

import spirite.base.pen.Penner
import spirite.base.util.linear.Rect

class PickBehavior(penner: Penner, val leftClock: Boolean) : PennerBehavior(penner) {

    fun pick() {
        val img = penner.renderEngine.renderWorkspace(penner.workspace ?: return)
        if( Rect(img.width, img.height).contains(penner.x, penner.y))
            penner.paletteManager.activeBelt.setColor( if( leftClock) 0 else 1, img.getColor(penner.x, penner.y))
    }

    override fun onStart() {pick()}
    override fun onTock() {}
    override fun onMove() { pick()}
}