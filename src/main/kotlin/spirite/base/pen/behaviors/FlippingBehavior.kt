package spirite.base.pen.behaviors

import rb.vectrix.mathUtil.MathUtil
import rb.vectrix.mathUtil.f
import spirite.base.imageData.drawer.IImageDrawer.IFlipModule
import spirite.base.pen.Penner
import kotlin.math.abs


class FlippingBehavior ( penner: Penner, val drawer: IFlipModule) : PennerBehavior(penner) {
    var startX = 0
    var startY = 0

    override fun onStart() {
        startX = penner.x
        startY = penner.y
    }
    override fun onTock() {}
    override fun onMove() {}

    override fun onPenUp() {
        val horizontal = MathUtil.distance(penner.x.f, penner.y.f, startX.f, startY.f) < 5 ||
                abs(penner.x - startX) > abs( penner.y - startY)
        drawer.flip(horizontal)

        super.onPenUp()
    }
}