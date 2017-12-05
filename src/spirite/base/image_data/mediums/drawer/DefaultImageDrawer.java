package spirite.base.image_data.mediums.drawer;

import spirite.base.brains.tools.ToolSchemes;
import spirite.base.brains.tools.ToolSchemes.MagneticFillMode;
import spirite.base.graphics.GraphicsContext;
import spirite.base.graphics.GraphicsContext.Composite;
import spirite.base.graphics.GraphicsDrawer;
import spirite.base.graphics.IImage;
import spirite.base.graphics.RawImage;
import spirite.base.image_data.GroupTree.LayerNode;
import spirite.base.image_data.GroupTree.Node;
import spirite.base.image_data.ImageWorkspace;
import spirite.base.image_data.ImageWorkspace.BuildingMediumData;
import spirite.base.image_data.UndoEngine;
import spirite.base.image_data.UndoEngine.ImageAction;
import spirite.base.image_data.UndoEngine.UndoableAction;
import spirite.base.image_data.layers.Layer;
import spirite.base.image_data.mediums.ABuiltMediumData;
import spirite.base.image_data.mediums.IMedium;
import spirite.base.image_data.mediums.drawer.IImageDrawer.*;
import spirite.base.image_data.selection.ALiftedData;
import spirite.base.image_data.selection.FlatLiftedData;
import spirite.base.image_data.selection.SelectionEngine;
import spirite.base.image_data.selection.SelectionMask;
import spirite.base.pen.PenTraits.PenState;
import spirite.base.pen.StrokeEngine;
import spirite.base.pen.StrokeEngine.STATE;
import spirite.base.pen.StrokeEngine.StrokeParams;
import spirite.base.util.Colors;
import spirite.base.util.MUtil;
import spirite.base.util.compaction.FloatCompactor;
import spirite.base.util.linear.MatTrans;
import spirite.base.util.linear.Vec2;
import spirite.base.util.linear.Vec2i;
import spirite.hybrid.DirectDrawer;
import spirite.hybrid.HybridHelper;
import spirite.hybrid.MDebug;
import spirite.hybrid.MDebug.ErrorType;
import spirite.hybrid.MDebug.WarningType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class DefaultImageDrawer 
	implements 	IImageDrawer,
				IFillModule,
				IClearModule,
				IFlipModule,
				IColorChangeModule,
				IInvertModule,
				ITransformModule,
				IStrokeModule,
				IMagneticFillModule,
				ILiftSelectionModule,
				IAnchorLiftModule
{
	private BuildingMediumData building;
	//private final IMedium img;
	
	public DefaultImageDrawer( IMedium img, BuildingMediumData building) {
		//this.img = img;
		this.building = building;
	}

	// ===============
	// ==== Queued Selection
	// Because many drawing actions can filter based on Selection
	// Mask, when re-doing them the mask which was active at the time
	// has to be remembered.  This function will apply the selection mask
	// to the next draw action performed.  If there is no seletion mask
	// queued, it will use the active selection.
	
	private void queueSelectionMask( SelectionMask mask) {
		queuedSelection = mask;
	}
	private SelectionMask pollSelectionMask(ImageWorkspace workspace) {
		if( queuedSelection == null)
			return workspace.getSelectionEngine().getSelection();

		SelectionMask ret = queuedSelection;
		queuedSelection = null;
		return ret;
	}
	private SelectionMask queuedSelection = null;

	// :::: IFillModule
	@Override
	public boolean fill(int x, int y, int color, BuildingMediumData _data) {
		if( _data == null) return false;
		ImageWorkspace workspace = _data.handle.getContext();
		SelectionEngine selectionEngine = workspace.getSelectionEngine();
		SelectionMask mask = selectionEngine.getSelection();

		AtomicReference<Boolean> aborted = new AtomicReference<>(false);
		AtomicReference<Integer> bgREF = new AtomicReference<>(0);
		_data.doOnBuiltData((built) -> {

			
			Vec2i p = built.convert( new Vec2i(x,y));
			
			built.doOnRaw((bi) -> {
				if( !MUtil.coordInImage( p.x, p.y, bi)) {
					aborted.set(true);
					return;
				}
				
				if( mask != null && !mask.contains(x, y)) {
					aborted.set(true);
					return;
				}
				
				bgREF.set(bi.getRGB(p.x, p.y));
				
				if( bgREF.get() == color) {
					aborted.set(true);
					return ;
				}
			});
		});
		if( aborted.get()) return false;
		
		final int bg = bgREF.get();

		workspace.getUndoEngine().performAndStore( new MaskedImageAction(_data, mask) {
			@Override
			protected void performImageAction(ABuiltMediumData built) {
				Vec2i p = built.convert( new Vec2i(x,y));
				if( mask == null) {
					built.doOnRaw((raw) -> {
						DirectDrawer.fill( raw, p.x, p.y, color);
					});
				}
				else {
					RawImage img = mask.liftSelectionFromData(built);
					Vec2i layerSpace = new Vec2i(p.x - mask.getOX(), p.y - mask.getOY());
					
					RawImage intermediate = null;
					
					if( bg == 0) {
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
						mask.drawMask(gc, true);
						gc.setComposite( Composite.SRC_OVER, 1.0f);
						gc.drawImage(intermediate, 0, 0 );
					}
					
					DirectDrawer.fill(img, layerSpace.x, layerSpace.y, color);

					if( bg == 0) { 
						// Continuing from above, after the fill is done, crop out the
						//	green outer mask out of the result image.  (This requires
						//	re-using the second BufferedImage since selection masks will
						//	most often be using a geometric rendering that never actually
						//	touches the pixels outside of it with its rasterizer)
						GraphicsContext gc = intermediate.getGraphics();
						gc.clear();
						mask.drawMask(gc, true);
						
						gc = img.getGraphics();
						gc.setComposite( Composite.DST_IN, 1.0f);;
						gc.drawImage(intermediate, 0, 0 );
					}
					
					final RawImage img_ = img;

					// Anchor the lifted image to the real image
					built.doOnGC((gc) -> {
						Vec2i p2 = built.convert(new Vec2i(mask.getOX(),mask.getOY()));
						gc.drawImage( img_, p2.x, p2.y);
						
					});
				}
			}
			
			@Override public String getDescription() {return "Fill";}
		});

		return true;
	}

	// :::: IClearModule
	@Override
	public void clear() {
		final ImageWorkspace workspace = building.handle.getContext();
		SelectionMask sel = pollSelectionMask(workspace);
		workspace.getUndoEngine().performAndStore(new MaskedImageAction(building, sel) {
			@Override
			protected void performImageAction(ABuiltMediumData built) {
				clearUnderSelection( built, sel);
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

		SelectionMask sel = selectionEngine.getSelection();
		
		if( selectionEngine.isLifted()) {
			MatTrans trans = new MatTrans();
			if( horizontal)
				trans.scale(-1, 1);
			else
				trans.scale(1, -1);
			selectionEngine.transformSelection(trans);
		}
		else if( sel == null)
			undoEngine.performAndStore( new FlipAction(building, selectionEngine.getSelection(), horizontal));
		else {
			UndoableAction actions[] = new UndoableAction[2];
			actions[0] = new FlipAction(building, sel, horizontal);
			
			// This is kind of bad
			RawImage img = HybridHelper.createImage(workspace.getWidth(), workspace.getHeight());
			GraphicsContext gc = img.getGraphics();
			
			gc.setColor( Colors.WHITE);
			gc.fillRect(0, 0, workspace.getWidth(), workspace.getHeight());
			
			img = sel.liftRawImage(img, 0, 0);
			
			img = flipImage(img, horizontal);
			
			//SelectionMask sel2 =  new BuiltSelection( img);
			//sel2 = new BuiltSelection( sel2.selection, sel2.offsetX+sel.offsetX, sel2.offsetX+sel.offsetY);
			//actions[1] = selectionEngine.createNewSelectAction(sel2);
			
			//undoEngine.performAndStore( undoEngine.new CompositeAction(Arrays.asList(actions), actions[0].getDescription()));
		}
	}
	public class FlipAction extends MaskedImageAction 
	{
		private final boolean horizontal;
		private FlipAction(BuildingMediumData data, SelectionMask mask, boolean horizontal) {
			super(data, mask);
			this.horizontal = horizontal;
			description = "Flip Action";
		}

		@Override
		protected void performImageAction(ABuiltMediumData built) {
			
			if( mask != null) {
				
				RawImage lifted = mask.liftSelectionFromData(built);

				RawImage buffer = flipImage(lifted, horizontal);

				built.doOnGC((gc) -> {
					gc.setComposite( Composite.DST_OUT, 1.0f);
					mask.drawMask( gc, true);

					gc.setComposite(Composite.SRC_OVER, 1.0f);
					gc.drawImage(buffer, mask.getOX(), mask.getOY());
					
					buffer.flush();
				});
			}
			else {
				built.doOnRaw((raw) -> {
					RawImage buffer = flipImage( raw, horizontal);
					
					GraphicsContext gc = raw.getGraphics();
					gc.setComposite( Composite.SRC, 1.0f);
					gc.drawImage(buffer, 0, 0);
					buffer.flush();
				});
			}
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
	public void changeColor( int from, int to, ToolSchemes.ColorChangeScopes scope, ToolSchemes.ColorChangeMode mode) {
		ImageWorkspace workspace = building.handle.getContext();
		SelectionEngine selectionEngine = workspace.getSelectionEngine();
		UndoEngine undoEngine = workspace.getUndoEngine();

		SelectionMask mask = selectionEngine.getSelection();
		
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
		private PerformFilterAction(BuildingMediumData data, SelectionMask mask) {
			super(data, mask);
		}

		@Override
		protected void performImageAction(ABuiltMediumData built) {
			if( mask != null) {
				// Lift the Selection
				RawImage lifted = mask.liftSelectionFromData(built);
				applyFilter(lifted);

				built.doOnGC((gc) -> {
					gc.setComposite( Composite.DST_OUT, 1.0f);
					mask.drawMask(gc, true);

					gc.setComposite( Composite.SRC_OVER, 1.0f);
					gc.drawImage( lifted, mask.getOX(), mask.getOY());
				});
			}
			else {
				built.doOnRaw((raw) -> {applyFilter(raw);});
			}
		}
		
		abstract void applyFilter( RawImage image);
		
	}
	public class ColorChangeAction extends PerformFilterAction 
	{
		private final int from, to;
		private final ToolSchemes.ColorChangeMode mode;
		private ColorChangeAction(
				BuildingMediumData data, 
				SelectionMask mask, 
				int from, int to, 
				ToolSchemes.ColorChangeMode mode) 
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
		SelectionMask mask = selectionEngine.getSelection();
		
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
		SelectionMask mask = selectionEngine.getSelection();

		undoEngine.performAndStore(new MaskedImageAction(building, mask) {
			@Override
			protected void performImageAction(ABuiltMediumData built) {
				built.doOnRaw((raw) -> {
					RawImage img2 = raw.deepCopy();
					GraphicsContext gc = raw.getGraphics();
					gc.clear();
					gc.setTransform(trans);
					gc.drawImage(img2, 0, 0);
				});
			}
			@Override public String getDescription() {return "Transform Layer";}
		});
	}

	

	private StrokeEngine activeEngine = null;

	@Override public StrokeEngine getStrokeEngine() {return activeEngine;}
	@Override public boolean canDoStroke(StrokeEngine.Method method) {return true;}

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
				SelectionMask mask, 
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
	public void endMagneticFill(final int color, final MagneticFillMode mode) {
		final ImageWorkspace workspace = building.handle.getContext();
		SelectionMask sel = pollSelectionMask(workspace);
		final float[] fill_x = fx.toArray();
		final float[] fill_y = fy.toArray();
		workspace.getUndoEngine().performAndStore(new MaskedImageAction(building, sel) {
			@Override
			protected void performImageAction(ABuiltMediumData built) {
				
				built.doOnGC((gc) -> {					
					gc.setColor(color);
					if( mode == MagneticFillMode.BEHIND)
						gc.setComposite(Composite.DST_OVER, 1);
					gc.fillPolygon(fill_x, fill_y, fill_x.length);
				});
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

		Vec2 start = building.trans.transform(new Vec2(x,y));
		int sx= Math.round(start.x);
		int sy = Math.round(start.y);
		
		building.doOnBuiltData((built) -> {
			built.doOnRaw((img) -> {
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
			
			});
		});
		
//		}
	}
	private boolean tryPixel( int x, int y, IImage raw, int lockedColor, boolean locked) {
		if( fx.size() > 1 && x == fx.get( fx.size()-1) && y == fy.get(fy.size()))
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

	// ::: ILiftSelectionModule
	@Override
	public ALiftedData liftSelection(final SelectionMask selection) {
		// Lift and Clear
		AtomicReference<ALiftedData> lifted = new AtomicReference<>(null);
		
		building.doOnBuiltData((built) -> {
			built.doOnRaw((raw) -> {
				lifted.set(new FlatLiftedData(selection.liftRawImage(raw, 0, 0)));
			});
		});
		
		UndoEngine ue = building.handle.getContext().getUndoEngine();
		
		ue.performAndStore(new MaskedImageAction(building, selection) {
			@Override
			protected void performImageAction(ABuiltMediumData built) {
				clearUnderSelection(built, selection);
			}
			@Override
			public String getDescription() {
				return "Clear Data That Was Lifted";
			}
		});
		
		return lifted.get();
	}
	
	private void clearUnderSelection( ABuiltMediumData built, SelectionMask selection) {
		if( selection == null)
			built.doOnGC((gc) -> gc.clear());
		else {
			built.doOnGC((gc) -> {
				gc.setComposite( Composite.DST_OUT, 1.0f);
				selection.drawMask(gc, true);
			});
		}
	}

	// :: IAnchorLiftModule
	@Override
	public boolean acceptsLifted(ALiftedData lifted) {
		return (lifted != null);
	}

	@Override
	public void anchorLifted(final ALiftedData lifted, final MatTrans trans) {
		UndoEngine undoEngine = building.handle.getContext().getUndoEngine();
		
		undoEngine.performAndStore( new ImageAction(building) { 
			@Override
			protected void performImageAction(ABuiltMediumData built) {
				built.doOnGC((gc) ->  {
					gc.transform(trans);
					gc.drawImage( lifted.readonlyAccess(), 0, 0);
				});
			}
		});
	}
}
