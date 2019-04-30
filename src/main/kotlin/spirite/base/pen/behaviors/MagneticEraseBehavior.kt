package spirite.base.pen.behaviors

import spirite.base.imageData.drawer.IImageDrawer.IMagneticEraseModule
import spirite.base.pen.Penner

class MagneticEraseBehavior (penner: Penner, val drawer: IMagneticEraseModule) : PennerBehavior(penner) {
    override fun onStart() {
        drawer.erase(penner.xf, penner.yf)
    }
    override fun onTock() {}

    override fun onMove() {
        drawer.erase(penner.xf, penner.yf)
    }

}