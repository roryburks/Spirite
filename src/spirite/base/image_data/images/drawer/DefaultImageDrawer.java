package spirite.base.image_data.images.drawer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import com.sun.prism.paint.Color;

import spirite.base.brains.ToolsetManager.ColorChangeScopes;
import spirite.base.graphics.GraphicsContext;
import spirite.base.graphics.GraphicsContext.Composite;
import spirite.base.graphics.GraphicsDrawer;
import spirite.base.graphics.IImage;
import spirite.base.graphics.RawImage;
import spirite.base.image_data.GroupTree.LayerNode;
import spirite.base.image_data.GroupTree.Node;
import spirite.base.image_data.ImageWorkspace;
import spirite.base.image_data.ImageWorkspace.BuildingMediumData;
import spirite.base.image_data.SelectionEngine;
import spirite.base.image_data.SelectionEngine.BuiltSelection;
import spirite.base.image_data.UndoEngine;
import spirite.base.image_data.UndoEngine.UndoableAction;
import spirite.base.image_data.images.ABuiltMediumData;
import spirite.base.image_data.images.IMedium;
// Auto-complete include isn't working so this comment is a marker for easy finding
import spirite.base.image_data.images.drawer.IImageDrawer.IClearModule;
import spirite.base.image_data.images.drawer.IImageDrawer.IColorChangeModule;
import spirite.base.image_data.images.drawer.IImageDrawer.IFillModule;
import spirite.base.image_data.images.drawer.IImageDrawer.IFlipModule;
import spirite.base.image_data.images.drawer.IImageDrawer.IInvertModule;
import spirite.base.image_data.images.drawer.IImageDrawer.IStrokeModule;
import spirite.base.image_data.images.drawer.IImageDrawer.ITransformModule;
import spirite.base.image_data.images.drawer.IImageDrawer.IMagneticFillModule;
import spirite.base.image_data.layers.Layer;
import spirite.base.pen.PenTraits.PenState;
import spirite.base.pen.StrokeEngine;
import spirite.base.pen.StrokeEngine.STATE;
import spirite.base.pen.StrokeEngine.StrokeParams;
import spirite.base.util.Colors;
import spirite.base.util.MUtil;
import spirite.base.util.compaction.FloatCompactor;
import spirite.base.util.glmath.MatTrans;
import spirite.base.util.glmath.Vec2;
import spirite.base.util.glmath.Vec2i;
import spirite.hybrid.DirectDrawer;
import spirite.hybrid.HybridHelper;
import spirite.hybrid.MDebug;
import spirite.hybrid.MDebug.ErrorType;
import spirite.hybrid.MDebug.WarningType;;

public class DefaultImageDrawer 
	implements 	IImageDrawer,
				IFillModule,
				IClearModule,
				IFlipModule,
				IColorChangeModule,
				IInvertModule,
				ITransformModule,
				IStrokeModule,
				IMagneticFillModule
{
	private BuildingMediumData building;
	private final IMedium img;
	
	public DefaultImageDrawer( IMedium img, BuildingMediumData building) {
		this.img = img;
		this.building = building;
	}

	// ===============
	// ==== Queued Selection
	// Because many drawing actions can filter based on Selection
	// Mask, when re-doing them the mask which was active at the time
	// has to be remembered.  This function will apply the selection mask
	// to the next draw action performed.  If there is no seletion mask
	// queued, it will use the active selection.
	
	private void queueSelectionMask( BuiltSelection mask) {
		queuedSelection = mask;
	}
	private BuiltSelection pollSelectionMask(ImageWorkspace workspace) {
		if( queuedSelection == null)
			return workspace.getSelectionEngine().getBuiltSelection();

		BuiltSelection ret = queuedSelection;
		queuedSelection = null;
		return ret;
	}
	private BuiltSelection queuedSelection = null;

	// :::: IFillModule
	@Override
	public boolean fill(int x, int y, int color, BuildingMediumData _data) {
		if( _data == null) return false;
		ImageWorkspace workspace = _data.handle.getContext();
		SelectionEngine selectionEngine = workspace.getSelectionEngine();
		BuiltSelection mask = selectionEngine.getBuiltSelection();
		
		AtomicReference<Boolean> aborted = new AtomicReference<Boolean>(false);
		_data.doOnBuiltData((built) -> {

			
			Vec2i p = built.convert( new Vec2i(x,y));
			
			RawImage bi = built.checkoutRaw();
			if( !MUtil.coordInImage( p.x, p.y, bi)) {
				aborted.set(true);
				return;
			}
			
			if( mask.selection != null && !mask.selection.contains(x - mask.offsetX, y-mask.offsetY)) {
				aborted.set(true);
				return;
			}
			if( bi.getRGB( p.x, p.y) == color) {
				aborted.set(true);
				return ;
			}
			//built.checkin();	// Probably not needed as it's read-only
		});
		if( aborted.get()) return false;

		workspace.getUndoEngine().performAndStore( new MaskedImageAction(_data, mask) {
			@Override
			protected void performImageAction(ABuiltMediumData built) {
				RawImage img;
				Vec2i layerSpace;
				Vec2i p = built.convert( new Vec2i(x,y));
				if( mask.selection == null) {
					img = built.checkoutRaw();
					layerSpace = built.convert( new Vec2i(p.x, p.y));
				}
				else {
					img = mask.liftSelectionFromData(built);
					layerSpace = new Vec2i(p.x - mask.offsetX, p.y - mask.offsetY);
				}

				RawImage intermediate = null;

				int bg = img.getRGB(layerSpace.x, layerSpace.y);
				
				if( mask.selection != null && bg == 0){
					// A lot of work for a singular yet common case: 
					// When coloring into transparent data, create an image which has
					//	a color other than 0 (pure transparent) outside of its selection
					//	mask (this has to be done in a couple of renderings).
					intermediate = img;
					img = HybridHelper.createImage( img.getWidth(), img.getHeight());
					
					GraphicsContext gc = img.getGraphics();
					gc.setColor( Colors.GREEN);
					gc.fillRect(0, 0, img.getWidth(), img.getHeight());
					gc.setComposite( Composite.CLEAR, 1.0f);
					mask.selection.drawSelectionMask( gc);
					gc.setComposite( Composite.SRC_OVER, 1.0f);
					gc.drawImage(intermediate, 0, 0 );
//					gc.dispose();
				}

				DirectDrawer.fill(img, layerSpace.x, layerSpace.y, color);


				
				if( mask.selection != null) {
					if( bg == 0) { 
						// Continuing from above, after the fill is done, crop out the
						//	green outer mask out of the result image.  (This requires
						//	re-using the second BufferedImage since selection masks will
						//	most often be using a geometric rendering that never actually
						//	touches the pixels outside of it with its rasterizer)
						GraphicsContext gc = intermediate.getGraphics();
						gc.clear();
						mask.selection.drawSelectionMask( gc);
//						g2.dispose();
						
						gc = img.getGraphics();
//						g2 = (Graphics2D) bi.getGraphics();
						gc.setComposite( Composite.DST_IN, 1.0f);;
						gc.drawImage(intermediate, 0, 0 );
//						g2.dispose();
					}

					// Anchor the lifted image to the real image
					GraphicsContext gc = built.checkout();
					Vec2i p2 = built.convert(new Vec2i(mask.offsetX,mask.offsetY));
					gc.drawImage( img, p2.x, p2.y);
				}
				built.checkin();
			}
			
			@Override public String getDescription() {return "Fill";}
		});

		return true;
	}

	// :::: IClearModule
	@Override
	public void clear() {
		final ImageWorkspace workspace = building.handle.getContext();
		BuiltSelection sel = pollSelectionMask(workspace);
		workspace.getUndoEngine().performAndStore(new MaskedImageAction(building, sel) {
			@Override
			protected void performImageAction(ABuiltMediumData built) {
				if( mask.selection == null) {
					built.checkout().clear();
					built.checkin();
				}
				else {
					GraphicsContext gc = built.checkout();
					gc.translate(mask.offsetX, mask.offsetY);
					gc.setComposite(Composite.DST_OUT, 1);
					mask.selection.drawSelectionMask(gc);
					built.checkin();
				}
			}
			@Override public String getDescription() {return "Clear Layer";}
		});
	}

	// :::: IFlipModule
	@Override
	public void flip( boolean horizontal) {
		ImageWorkspace workspace = building.handle.getContext();
		SelectionEngine selectionEngine = workspace.getSelectionEngine();
		UndoEngine undoEngine = workspace.getUndoEngine();

		BuiltSelection sel = selectionEngine.getBuiltSelection();
		
		if( selectionEngine.isLifted()) {
			MatTrans trans = new MatTrans();
			if( horizontal)
				trans.scale(-1, 1);
			else
				trans.scale(1, -1);
			selectionEngine.transformSelection(trans);
		}
		else if( sel == null || sel.selection == null)
			undoEngine.performAndStore( new FlipAction(building, selectionEngine.getBuiltSelection(), horizontal));
		else {
			UndoableAction actions[] = new UndoableAction[2];
			actions[0] = new FlipAction(building, selectionEngine.getBuiltSelection(), horizontal);
			
			// This is kind of bad
			RawImage img = HybridHelper.createImage(workspace.getWidth(), workspace.getHeight());
			GraphicsContext gc = img.getGraphics();
			
			gc.setColor( Colors.WHITE);
			gc.fillRect(0, 0, workspace.getWidth(), workspace.getHeight());
//			g2.dispose();
			
			img = sel.liftSelectionFromImage(img, 0, 0);
			
			img = flipImage(img, horizontal);
			
			BuiltSelection sel2 =  new BuiltSelection( img);
			sel2 = new BuiltSelection( sel2.selection, sel2.offsetX+sel.offsetX, sel2.offsetX+sel.offsetY);
			actions[1] = selectionEngine.createNewSelectAction(sel2);
			
			undoEngine.performAndStore( undoEngine.new CompositeAction(Arrays.asList(actions), actions[0].getDescription()));
		}
	}
	public class FlipAction extends MaskedImageAction 
	{
		private final boolean horizontal;
		private FlipAction(BuildingMediumData data, BuiltSelection mask, boolean horizontal) {
			super(data, mask);
			this.horizontal = horizontal;
			description = "Flip Action";
		}

		@Override
		protected void performImageAction(ABuiltMediumData built) {
			
			if( mask != null && mask.selection != null) {
				
				RawImage lifted = mask.liftSelectionFromData(built);

				RawImage buffer = flipImage(lifted, horizontal);

				GraphicsContext gc = built.checkout();
				gc.setComposite( Composite.DST_OUT, 1.0f);
				mask.drawSelectionMask( gc);

				gc.setComposite(Composite.SRC_OVER, 1.0f);
				gc.drawImage(buffer, mask.offsetX, mask.offsetY);
//				gc.dispose();
				
				buffer.flush();
			}
			else {
				RawImage bi = built.checkoutRaw();
				RawImage buffer = flipImage( bi, horizontal);
				
				GraphicsContext gc = bi.getGraphics();
				gc.setComposite( Composite.SRC, 1.0f);
				gc.drawImage(buffer, 0, 0);
//				g2.dispose();
				buffer.flush();
			}
			built.checkin();
			
		}
	}
	private static RawImage flipImage( RawImage img, boolean horizontal) {
		// Might be able to do this single-Image but things get weird if you 
		//	draw a Buffer onto itself
		RawImage buffer = HybridHelper.createImage( img.getWidth(), img.getHeight());
		GraphicsContext gc = buffer.getGraphics();
		
		if( horizontal) {
			gc.translate(img.getWidth(), 0);
			gc.scale(-1.0, 1.0);
		}
		else {
			gc.translate(0, img.getHeight());
			gc.scale(1.0, -1.0);
		}
		gc.drawImage(img, 0, 0);
//		g2.dispose();
		
		return buffer;
	}


	// :::: IColorChangeModule
	@Override
	public void changeColor( int from, int to, ColorChangeScopes scope, int mode) {
		ImageWorkspace workspace = building.handle.getContext();
		SelectionEngine selectionEngine = workspace.getSelectionEngine();
		UndoEngine undoEngine = workspace.getUndoEngine();

		BuiltSelection mask = selectionEngine.getBuiltSelection();
		
		Node selected = null;
		
		switch( scope) {
		case LOCAL:
			BuildingMediumData bid = workspace.buildActiveData();
			if( bid != null) {
				undoEngine.performAndStore( new ColorChangeAction(bid, mask, from, to, mode));
			}
			break;
		case GROUP:
			// Switch statement is kind of awkward.  Just roll with it.
			selected = workspace.getSelectedNode();
			if( selected == null) return;
		case PROJECT:
			if( selected == null) 
				selected = workspace.getRootNode();

			List<UndoableAction> actions = new ArrayList<>();
			
			for( LayerNode lnode : selected.getAllLayerNodes()) {
				Layer layer = lnode.getLayer();
				
				for( BuildingMediumData data : layer.getDataToBuild()) {
					data.trans.preTranslate( lnode.getOffsetX(), lnode.getOffsetY());
					actions.add( new ColorChangeAction(
							data,
							mask, from, to, mode));
				}
			}
			
			UndoableAction action = undoEngine.new CompositeAction(actions, "Color Change Action");
			undoEngine.performAndStore(action);
			break;
		}
	}

	public abstract class PerformFilterAction extends MaskedImageAction
	{
		private PerformFilterAction(BuildingMediumData data, BuiltSelection mask) {
			super(data, mask);
		}

		@Override
		protected void performImageAction(ABuiltMediumData built) {
			if( mask != null && mask.selection != null) {
				// Lift the Selection
				RawImage lifted = mask.liftSelectionFromData(built);
				applyFilter(lifted);

				GraphicsContext gc = built.checkout();
				gc.setComposite( Composite.DST_OUT, 1.0f);
				mask.drawSelectionMask(gc);

				gc.setComposite( Composite.SRC_OVER, 1.0f);
				gc.drawImage( lifted, mask.offsetX, mask.offsetY );
			}
			else {
				RawImage bi = built.checkoutRaw();
				applyFilter(bi);
			}
			built.checkin();
		}
		
		abstract void applyFilter( RawImage image);
		
	}
	public class ColorChangeAction extends PerformFilterAction 
	{
		private final int from, to;
		private final int mode;
		private ColorChangeAction(
				BuildingMediumData data, 
				BuiltSelection mask, 
				int from, int to, 
				int mode) 
		{
			super(data, mask);
			this.from = from;
			this.to = to;
			this.mode = mode;
			description = "Color Change Action";
		}
		@Override
		void applyFilter(RawImage image) {
			GraphicsDrawer directDrawer = building.handle.getContext().getSettingsManager().getDefaultDrawer();
			directDrawer.changeColor(image, from, to, mode);
		}
	}
	
	// :::: IInvertModule
	@Override
	public void invert() {
		ImageWorkspace workspace = building.handle.getContext();
		SelectionEngine selectionEngine = workspace.getSelectionEngine();
		UndoEngine undoEngine = workspace.getUndoEngine();
		BuiltSelection mask = selectionEngine.getBuiltSelection();
		
		undoEngine.performAndStore( new PerformFilterAction(building, mask) {
			@Override void applyFilter(RawImage image) {
				GraphicsDrawer directDrawer = building.handle.getContext().getSettingsManager().getDefaultDrawer();
				directDrawer.invert(image);
			}
		});
	}

	// :::: ITramsformModule
	@Override
	public void transform(final MatTrans trans) {

		ImageWorkspace workspace = building.handle.getContext();
		SelectionEngine selectionEngine = workspace.getSelectionEngine();
		UndoEngine undoEngine = workspace.getUndoEngine();
		BuiltSelection mask = selectionEngine.getBuiltSelection();

		undoEngine.performAndStore(new MaskedImageAction(building, mask) {
			@Override
			protected void performImageAction(ABuiltMediumData built) {
				RawImage img = built.checkoutRaw();
				RawImage img2 = img.deepCopy();
				GraphicsContext gc = img.getGraphics();
				gc.clear();
				gc.setTransform(trans);
				gc.drawImage(img2, 0, 0);
				built.checkin();
			}
			@Override public String getDescription() {return "Transform Layer";}
		});
	}

	

	private StrokeEngine activeEngine = null;

	@Override public StrokeEngine getStrokeEngine() {return activeEngine;}
	@Override public boolean canDoStroke(StrokeParams params) {return true;}

	@Override
	public boolean startStroke(StrokeParams params, PenState ps) {
		ImageWorkspace workspace = building.handle.getContext();
		
		if( activeEngine != null) {
			MDebug.handleError(ErrorType.STRUCTURAL, "Tried to draw two strokes at once within the DrawEngine (if you need to do that, manually instantiate a separate StrokeEngine.");
			return false;
		}
		else {
			activeEngine = workspace.getSettingsManager().getDefaultDrawer().getStrokeEngine();
			
			if( activeEngine.startStroke( params, ps, building, pollSelectionMask(workspace)))
				building.handle.refresh();
			return true;
		}
	}

	@Override
	public void stepStroke(PenState ps) {
		if( activeEngine == null || activeEngine.getState() != STATE.DRAWING) {
			MDebug.handleWarning(WarningType.STRUCTURAL, this, "Tried to step stroke that isn't active.");
			return ;
		}
		else {
			if(activeEngine.stepStroke(ps))
				activeEngine.getImageData().handle.refresh();
		}
	}

	@Override
	public void endStroke() {
		if( activeEngine == null || activeEngine.getState() != STATE.DRAWING) {
			activeEngine = null;
			MDebug.handleWarning(WarningType.STRUCTURAL, this, "Tried to end stroke that isn't active.");
			return ;
		}
		else {
			activeEngine.endStroke();
				
			building.handle.getContext().getUndoEngine().storeAction(
				new StrokeAction(
					activeEngine,
					activeEngine.getParams(),
					activeEngine.getHistory(),
					activeEngine.getLastSelection(),
					building));
			activeEngine = null;
		}
	}
	public class StrokeAction extends MaskedImageAction {
		private final PenState[] points;
		private final StrokeEngine.StrokeParams params;
		private final StrokeEngine engine;
		
		StrokeAction( 
				StrokeEngine engine,
				StrokeEngine.StrokeParams params, 
				PenState[] points, 
				BuiltSelection mask, 
				BuildingMediumData data)
		{
			super(data, mask);
			this.engine = engine;
			this.params = params;
			this.points = points;
			
			switch( params.getMethod()) {
			case BASIC:
				description = "Basic Stroke Action";
				break;
			case ERASE:
				description = "Erase Stroke Action";
				break;
			case PIXEL:
				description = "Pixel Stroke Action";
				break;
			}
		}
		
		public StrokeEngine.StrokeParams getParams() {
			return params;
		}
		
		@Override
		public void performImageAction(ABuiltMediumData built ) {
			queueSelectionMask(mask);
			
			engine.batchDraw(params, points, built, mask);
		}
	}
	
	
	// :::: IMagneticFillModule
	FloatCompactor fx;
	FloatCompactor fy;
	@Override
	public void startMagneticFill() {
		fx = new FloatCompactor();
		fy = new FloatCompactor();
	}

	@Override
	public void endMagneticFill(final int color) {
		final ImageWorkspace workspace = building.handle.getContext();
		BuiltSelection sel = pollSelectionMask(workspace);
		final float[] fill_x = fx.toArray();
		final float[] fill_y = fy.toArray();
		workspace.getUndoEngine().performAndStore(new MaskedImageAction(building, sel) {
			@Override
			protected void performImageAction(ABuiltMediumData built) {
				
				GraphicsContext gc = built.checkout();
				gc.setColor(color);
				gc.fillPolygon(fill_x, fill_y, fill_x.length);
			}
			@Override
			public String getDescription() {
				return "Magnetic Fill";
			}
		
		});
		
		fx = null;
		fy = null;
	}

	@Override
	public void anchorPoints(float x, float y, float r, boolean locked, boolean relooping) {
		final ImageWorkspace workspace = building.handle.getContext();
		int lockedColor = (locked)?workspace.getPaletteManager().getActiveColor(0):0;

		Vec2 start = building.trans.transform(new Vec2(x,y), new Vec2());
		int sx= Math.round(start.x);
		int sy = Math.round(start.y);
		IImage img = building.handle.deepAccess();
		
		if( tryPixel( sx, sy, img, lockedColor, locked))
			return;
		
		for( int tr = 1; tr < r+1; ++tr) {
			for( int snake = 0; snake < tr; ++snake) {
				// Topleft->topright
				if( tryPixel( sx-tr+snake, sy-tr, img, lockedColor, locked))
					return;
				//TR->BR
				if( tryPixel(sx+tr, sy-tr+snake, img, lockedColor, locked))
					return;
				//BR->BL
				if( tryPixel(sx+tr-snake, sy+tr, img, lockedColor, locked))
					return;
				//BL->TL
				if( tryPixel(sx-tr, sy+tr-snake, img, lockedColor, locked))
					return;
			}
		}
	}
	private boolean tryPixel( int x, int y, IImage raw, int lockedColor, boolean locked) {
		if( x == fx.get( fx.size()-1) && y == fy.get(fy.size()))
			return false;
		int c = raw.getRGB(x, y);
		if( (locked && Colors.colorDistance(c, lockedColor) < 25) ||
				(!locked && Colors.getAlpha(c) > 230 )) 
		{
			fx.add(x);
			fy.add(y);
			return true;
		}
		return false;
	}

	@Override
	public void erasePoints(float x, float y, float r) {
		// TODO Auto-generated method stub
	}

	@Override
	public float[] getMagFillXs() {
		return fx.toArray();
	}

	@Override
	public float[] getMagFillYs() {
		return fy.toArray();
	}
}
