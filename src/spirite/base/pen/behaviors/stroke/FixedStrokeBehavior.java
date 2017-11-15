package spirite.base.pen.behaviors.stroke;

import spirite.base.graphics.GraphicsContext;
import spirite.base.graphics.GraphicsContext.Composite;
import spirite.base.graphics.GraphicsContext.LineAttributes;
import spirite.base.image_data.mediums.drawer.IImageDrawer;
import spirite.base.image_data.mediums.drawer.IImageDrawer.IStrokeModule;
import spirite.base.pen.PenTraits.PenState;
import spirite.base.pen.Penner;
import spirite.base.pen.StrokeEngine.StrokeParams;
import spirite.base.pen.behaviors.DrawnStateBehavior;
import spirite.hybrid.HybridHelper;

public abstract class FixedStrokeBehavior extends DrawnStateBehavior{
	int startX, startY;
	StrokeParams params;
	
	public FixedStrokeBehavior( Penner penner) {
		super(penner);
	}

	@Override
	public void paintOverlay(GraphicsContext gc) {
		gc.pushTransform();
		gc.setTransform(penner.view.getViewTransform());
		gc.setComposite(Composite.SRC_OVER, 1);
		gc.setColor(params.getColor());
		gc.drawLine(startX, startY, penner.x, penner.y);
		gc.popTransform();
	}
	
	public abstract StrokeParams makeStroke();

	@Override
	public void start() {
		startX = penner.x;
		startY = penner.y;
		this.params = makeStroke();
	}

	@Override public void onTock() {}
	@Override public void onMove() {}
	
	@Override
	public void end() {
		IImageDrawer drawer = this.penner.workspace.getActiveDrawer();
		if( drawer instanceof IStrokeModule) {
			IStrokeModule stroke = (IStrokeModule)drawer;
			stroke.startStroke(params, new PenState(startX, startY, 1));
			stroke.stepStroke(new PenState(penner.x, penner.y, 1));
			stroke.endStroke();
		}else {
			HybridHelper.beep();
		}
		
		super.end();
	}
}
