package spirite.base.pen.behaviors;

import spirite.base.image_data.mediums.drawer.IImageDrawer;
import spirite.base.image_data.mediums.drawer.IImageDrawer.IStrokeModule;
import spirite.base.pen.PenTraits.PenState;
import spirite.base.pen.Penner;
import spirite.base.pen.StrokeEngine;
import spirite.hybrid.HybridHelper;

abstract class StrokeBehavior extends StateBehavior {
	StrokeBehavior(Penner penner) {
		super(penner);
	}

	int shiftX = this.penner.rawX;
	int shiftY = this.penner.rawY;
	int dx = this.penner.x;
	int dy = this.penner.y;
	private int shiftMode = -1;	// 0 : accept any, 1 : horizontal, 2: vertical
	protected IStrokeModule drawer;
	
	public void startStroke (StrokeEngine.StrokeParams stroke) {
		if( this.penner.workspace != null && this.penner.workspace.buildActiveData() != null) {
			shiftX = this.penner.rawX;
			shiftY = this.penner.rawY;
			
			IImageDrawer drawer = this.penner.workspace.getActiveDrawer();
			if( drawer instanceof IStrokeModule 
					&& ((IStrokeModule) drawer).canDoStroke(stroke.getMethod())
					&&((IStrokeModule)drawer).startStroke(stroke, new PenState(this.penner.x,this.penner.y,this.penner.pressure))) 
			{
				this.drawer = (IStrokeModule) drawer;
				this.penner.workspace.setActiveStrokeEngine(this.drawer.getStrokeEngine());
			}
			else {
				end();
				HybridHelper.beep();
			}
		}
		else {
			end();
			HybridHelper.beep();
		}
	}
	
	@Override
	public void onTock() {
		if( this.penner.holdingShift) {
			if( shiftMode == -1) {
				shiftMode = 0;
				shiftX = this.penner.rawX;
				shiftY = this.penner.rawY;
			}
			if( shiftMode == 0) {
				if( Math.abs(shiftX - this.penner.rawX) > 10)
					shiftMode = 1;
				else if( Math.abs(shiftY - this.penner.rawY) > 10)
					shiftMode = 2;
			}
			
			if( shiftMode == 1)
				dx = this.penner.x;
			if( shiftMode == 2)
				dy = this.penner.y;
		}
		else {
			shiftMode = -1;
			dx = this.penner.x;
			dy = this.penner.y;
		}
		drawer.stepStroke( new PenState( dx, dy, this.penner.pressure));

	}

	@Override
	public void onPenUp() {
		drawer.endStroke();
		super.onPenUp();
	}
	
	@Override public void onMove() {}
	
	@Override
	public void end() {
		this.penner.workspace.setActiveStrokeEngine(null);
		super.end();
	}
}