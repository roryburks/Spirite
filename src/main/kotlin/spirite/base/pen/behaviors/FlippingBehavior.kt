package spirite.base.pen.behaviors

import spirite.base.imageData.drawer.IImageDrawer.IFlipModule
import spirite.base.pen.Penner
import spirite.base.util.MathUtil
import rb.vectrix.mathUtil.f
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

/**
public class FlippingBehavior extends StateBehavior {
public FlippingBehavior(Penner penner) {
super(penner);
}
int startX, startY;

@Override
public void start() {
startX = this.penner.xi;
startY = this.penner.yi;
}
@Override
public void onMove() {

}
@Override
public void onPenUp() {
boolean horizontal = MUtil.distance(this.penner.xi , this.penner.yi, startX, startY) < 5
||Math.abs(this.penner.xi - startX) > Math.abs(this.penner.yi - startY);
this.penner.tryFlip( horizontal);

super.onPenUp();
}
@Override public void onTock() {}


}*/