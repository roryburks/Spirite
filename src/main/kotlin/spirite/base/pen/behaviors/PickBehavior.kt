package spirite.base.pen.behaviors

import spirite.base.pen.Penner
import spirite.base.util.ImageUtil

class PickBehavior(penner: Penner, val leftClock: Boolean) : PennerBehavior(penner) {

    fun pick() {
        val img = penner.renderEngine.renderWorkspace(penner.workspace ?: return)
        if( ImageUtil.coordsInImage( penner.x, penner.y, img))
            penner.paletteManager.setActiveColor( if( leftClock) 0 else 1, img.getColor(penner.x, penner.y))
    }

    override fun onStart() {pick()}
    override fun onTock() {}
    override fun onMove() { pick()}
}