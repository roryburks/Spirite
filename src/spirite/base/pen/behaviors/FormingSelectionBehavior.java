package spirite.base.pen.behaviors;

import spirite.base.brains.ToolsetManager.BoxSelectionShape;
import spirite.base.graphics.GraphicsContext;
import spirite.base.image_data.selection.SelectionEngine.BuildMode;
import spirite.base.image_data.selection.SelectionMask;
import spirite.base.pen.Penner;
import spirite.base.pen.selection_builders.ASelectionBuilder;
import spirite.base.pen.selection_builders.OvalSelectionBuilder;
import spirite.base.pen.selection_builders.RectSelectionBuilder;
import spirite.base.util.Colors;
import spirite.base.util.glmath.MatTrans;

public class FormingSelectionBehavior extends DrawnStateBehavior {
		private final BoxSelectionShape shape;
		private final BuildMode mode;
		private ASelectionBuilder builder;
		
		public FormingSelectionBehavior( Penner penner, BoxSelectionShape shape, BuildMode mode) {
			super(penner);
			this.shape = shape;
			this.mode = mode;
		}
		@Override
		public void start() {
			
			switch( shape) {
			case RECTANGLE:
				builder = new RectSelectionBuilder(this.penner.workspace);
				//builder = selectionEngine.new RectSelectionBuilder();
				break;
			case OVAL:
				builder = new OvalSelectionBuilder(this.penner.workspace);
				//builder = selectionEngine.new OvalSelectionBuilder();
				break;
			}
			
			builder.start(this.penner.x, this.penner.y);
			//selectionEngine.startBuildingSelection( builder, x, y, mode);
		}
		@Override
		public void onMove() {
			builder.update(this.penner.x, this.penner.y);
			//selectionEngine.updateBuildingSelection(x, y);
		}
		@Override
		public void onPenUp() {
			this.penner.selectionEngine.mergeSelection(new SelectionMask(builder.build()), mode);
			super.onPenUp();
		}
		@Override
		public void onTock() {
//			selectionEngine.updateBuildingSelection(x, y);
		}
		
		@Override
		public void paintOverlay(GraphicsContext gc) {
			if( builder != null) {
	            MatTrans trans = new MatTrans(gc.getTransform());
	        	gc.preTransform(this.penner.view.getViewTransform());
				gc.setColor( Colors.BLACK);
				builder.draw(gc);
				gc.setTransform(trans);
			}
		}
	}