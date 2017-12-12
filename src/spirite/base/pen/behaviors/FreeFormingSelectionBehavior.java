package spirite.base.pen.behaviors;

import spirite.base.graphics.GraphicsContext;
import spirite.base.image_data.selection.SelectionEngine.BuildMode;
import spirite.base.image_data.selection.SelectionMask;
import spirite.base.pen.Penner;
import spirite.base.pen.selection_builders.FreeformSelectionBuilder2;
import spirite.base.util.Colors;
import spirite.base.util.MUtil;
import spirite.base.util.linear.MutableTransform;
import spirite.base.util.linear.Vec2i;

public class FreeFormingSelectionBehavior extends DrawnStateBehavior {
	private boolean drawing = true;
	private final BuildMode mode;
	private FreeformSelectionBuilder2 builder;
	public FreeFormingSelectionBehavior( Penner penner, BuildMode mode) {
		super(penner);
		this.mode = mode;
	}
	@Override
	public void start() {
		builder = new FreeformSelectionBuilder2(this.penner.workspace);
		builder.start(this.penner.x, this.penner.y);
		//builder = selectionEngine.new FreeformSelectionBuilder();
		//selectionEngine.startBuildingSelection( builder, x, y, mode);
	}

	@Override
	public void onMove() {
		if( drawing && (this.penner.x != this.penner.oldX || this.penner.y != this.penner.oldY))
			builder.update(this.penner.x, this.penner.y);
			//selectionEngine.updateBuildingSelection(x, y);
	}
	@Override public void onTock() {}
	public boolean testFinish() {
		Vec2i p_s = builder.getStart();
		if( MUtil.distance(p_s.getX(), p_s.getY(), this.penner.x, this.penner.y)<=5) {
			this.penner.selectionEngine.mergeSelection(new SelectionMask(builder.build()), mode);
			this.end();
			return true;
		}
		return false;
	}
	@Override public void onPenUp() {
		drawing = false;
		testFinish();
	}
	@Override
	public void onPenDown() {
		drawing = true;
		if( !testFinish())
			builder.update(this.penner.x, this.penner.y);
			//selectionEngine.updateBuildingSelection(x, y);
	}
	@Override
	public void paintOverlay(GraphicsContext g) {
        MutableTransform trans = g.getTransform().toMutable();
    	g.preTransform(this.penner.view.getViewTransform());
		g.setColor( Colors.BLACK);
		builder.draw(g);
		g.setTransform(trans);
		
		if( !drawing) {
			Vec2i p_e = builder.getEnd();
			
			g.setColor( Colors.BLACK);
			g.drawLine(this.penner.view.itsXm(p_e.getX()), this.penner.view.itsYm(p_e.getY()),
					this.penner.view.itsXm(this.penner.x), this.penner.view.itsYm(this.penner.y));

		}

		Vec2i p_s = builder.getStart();
		if( MUtil.distance(p_s.getX(), p_s.getY(), this.penner.x, this.penner.y)<=5) {
			g.setColor( Colors.YELLOW);
			g.fillOval(this.penner.view.itsXm(p_s.getX())-5, this.penner.view.itsYm(p_s.getY()) - 5, 10, 10);
		}
		else
			g.drawOval(this.penner.view.itsXm(p_s.getX())-5, this.penner.view.itsYm(p_s.getY()) - 5, 10, 10);
	}
}