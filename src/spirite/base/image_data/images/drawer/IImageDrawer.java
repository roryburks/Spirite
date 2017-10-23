package spirite.base.image_data.images.drawer;

import spirite.base.brains.ToolsetManager.ColorChangeScopes;
import spirite.base.image_data.ImageWorkspace.BuildingImageData;
import spirite.base.image_data.SelectionEngine.BuiltSelection;
import spirite.base.image_data.UndoEngine.ImageAction;
import spirite.base.pen.PenTraits.PenState;
import spirite.base.pen.StrokeEngine;

public interface IImageDrawer {

	public abstract class MaskedImageAction extends ImageAction {
		protected final BuiltSelection mask;

		protected MaskedImageAction(BuildingImageData data, BuiltSelection mask) {
			super(data);
			this.mask = mask;
		}
	}
	
	// Modules, an Image Drawer may implement these or they may not.  Not implementing them means
	//	that the Drawer is incapable of performing these draw actions (e.g. because it doesn't 
	//	make sense for the data type).
	public interface IStrokeModule {
		public boolean strokeIsDrawing();
		public StrokeEngine getStrokeEngine();
		public boolean startStroke( StrokeEngine.StrokeParams params, PenState ps, BuildingImageData bis);
		public boolean stepStroke( PenState ps);
		public void endStroke();
	}
	
	public interface IClearModule {
		public void clear( BuildingImageData data);
	}
	
	public interface IFillModule {
		public boolean fill( int x, int y, int color, BuildingImageData _data);
	}
	
	public interface IFlipModule {
		public void flip( BuildingImageData data, boolean horizontal);
	}
	
	public interface IColorChangeModule {
		public void changeColor( int from, int to, ColorChangeScopes scope, int mode);
	}
	
	public interface IInvertModule {
		public void invert(BuildingImageData data);
	}
}
