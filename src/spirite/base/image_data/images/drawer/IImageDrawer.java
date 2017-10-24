package spirite.base.image_data.images.drawer;

import spirite.base.brains.ToolsetManager.ColorChangeScopes;
import spirite.base.graphics.gl.GLGeom.Primitive;
import spirite.base.image_data.ImageWorkspace.BuildingImageData;
import spirite.base.image_data.SelectionEngine.BuiltSelection;
import spirite.base.image_data.UndoEngine.ImageAction;
import spirite.base.pen.PenTraits.PenState;
import spirite.base.pen.StrokeEngine;
import spirite.base.util.glmath.MatTrans;

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
		//public boolean strokeIsDrawing();	// SHOULD be true iff getStrokeEngine = null
		public StrokeEngine getStrokeEngine();
		public boolean canDoStroke( StrokeEngine.StrokeParams params);	// EG: some Drawers might be able to erase, but not draw
		public boolean startStroke( StrokeEngine.StrokeParams params, PenState ps);
		public void stepStroke( PenState ps);
		public void endStroke();
	}
	
	public interface IClearModule {
		public void clear();
	}
	
	public interface IFillModule {
		public boolean fill( int x, int y, int color, BuildingImageData _data);
	}
	
	public interface IFlipModule {
		public void flip( boolean horizontal);
	}
	
	public interface IColorChangeModule {
		public void changeColor(  int from, int to, ColorChangeScopes scope, int mode);
	}
	
	public interface IInvertModule {
		public void invert();
	}
	
	public interface ITransformModule {
		public void transform( MatTrans trans);
	}
	
	public interface IMagneticFillModule {
		public void startMagneticFill();
		public void endMagneticFill();
		public float[][] anchorPoints( float x1, float y1, float r1, float x2, float y2, float r2);
		public float[][] anchorPointsHard( float x, float y, float r);
		//public float[] interpretFillPoint( int x, int y, float weight);
		//public float[] rightExistingPoints( float[] x, float[] y);
		
		public void interpretFill( float[] curveRaw, int color);
	}
}
