package spirite.base.pen.behaviors

import spirite.base.imageData.drawer.IImageDrawer.IFlipModule
import spirite.base.pen.Penner
import spirite.base.util.MathUtil
import spirite.base.util.f
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
startX = this.penner.x;
startY = this.penner.y;
}
@Override
public void onMove() {

}
@Override
public void onPenUp() {
boolean horizontal = MUtil.distance(this.penner.x , this.penner.y, startX, startY) < 5
||Math.abs(this.penner.x - startX) > Math.abs(this.penner.y - startY);
this.penner.tryFlip( horizontal);

super.onPenUp();
}
@Override public void onTock() {}


}*/