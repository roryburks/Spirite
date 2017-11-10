package spirite.base.pen.behaviors;

import spirite.base.graphics.GraphicsContext;
import spirite.base.image_data.mediums.drawer.IImageDrawer.IMagneticFillModule;
import spirite.base.pen.Penner;

public class MagFillingBehavior extends DrawnStateBehavior {
	IMagneticFillModule drawer;
	
	public MagFillingBehavior( Penner penner, IMagneticFillModule drawer) {
		super(penner);
		this.drawer = drawer;
	}
	
	@Override
	public void paintOverlay(GraphicsContext gc) {
		gc.setTransform(this.penner.view.getViewTransform());
		gc.setColor(0xFFFFFF ^ this.penner.paletteManager.getActiveColor(0));

		float[] fx = drawer.getMagFillXs();
		float[] fy = drawer.getMagFillYs();
		
		gc.drawPolyLine(fx, fy, fx.length);
	}

	@Override public void start() {
		drawer.startMagneticFill();
	}
	@Override public void onTock() {}
	@Override
	public void onMove() {
		drawer.anchorPoints(penner.x, penner.y, 10, penner.holdingShift, penner.holdingCtrl);
	}
	@Override
	public void onPenUp() {
		//drawer.anchorPointsHard(x, y, 5);
		//drawer.interpretFill(fc.toArray(), paletteManager.getActiveColor(0));
		drawer.endMagneticFill(this.penner.paletteManager.getActiveColor(0));
		super.onPenUp();
	}
}