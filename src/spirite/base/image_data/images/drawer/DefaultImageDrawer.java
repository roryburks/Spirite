package spirite.base.image_data.images.drawer;

import java.util.Arrays;

import spirite.base.graphics.GraphicsContext;
import spirite.base.graphics.RawImage;
import spirite.base.graphics.GraphicsContext.Composite;
import spirite.base.image_data.ImageWorkspace;
import spirite.base.image_data.ImageWorkspace.BuildingImageData;
import spirite.base.image_data.SelectionEngine;
import spirite.base.image_data.UndoEngine;
import spirite.base.image_data.SelectionEngine.BuiltSelection;
import spirite.base.image_data.UndoEngine.UndoableAction;
import spirite.base.image_data.images.IBuiltImageData;
import spirite.base.image_data.images.IInternalImage;
import spirite.base.image_data.images.drawer.IImageDrawer.*;	// Bad, but auto-complete include isn't working with my Eclipse
import spirite.base.util.Colors;
import spirite.base.util.MUtil;
import spirite.base.util.glmath.MatTrans;
import spirite.base.util.glmath.Vec2i;
import spirite.hybrid.DirectDrawer;
import spirite.hybrid.HybridHelper;;

public class DefaultImageDrawer 
	implements 	IImageDrawer,
				IFillModule,
				IClearModule,
				IFlipModule
{
	private final IInternalImage img;
	
	public DefaultImageDrawer( IInternalImage img) {
		this.img = img;
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
	public boolean fill(int x, int y, int color, BuildingImageData _data) {
		if( _data == null) return false;
		
		ImageWorkspace workspace = _data.handle.getContext();
		SelectionEngine selectionEngine = workspace.getSelectionEngine();
		IBuiltImageData data = workspace.buildData(_data);
		
		Vec2i p = data.convert( new Vec2i(x,y));
		
		RawImage bi = data.checkoutRaw();
		if( !MUtil.coordInImage( p.x, p.y, bi)) {
			return false;
		}
		
		BuiltSelection mask = selectionEngine.getBuiltSelection();
		if( mask.selection != null && !mask.selection.contains(x - mask.offsetX, y-mask.offsetY)) {
			return false;
		}
		if( bi.getRGB( p.x, p.y) == color) {
			return false;
		}
		data.checkin();

		workspace.getUndoEngine().performAndStore( new MaskedImageAction(_data, mask) {
			@Override
			protected void performImageAction() {
				RawImage img;
				Vec2i layerSpace;
				IBuiltImageData built = workspace.buildData(builtImage);
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
					Vec2i p = built.convert(new Vec2i(mask.offsetX,mask.offsetY));
					gc.drawImage( img, p.x, p.y);
				}
				built.checkin();
			}
			
			@Override public String getDescription() {return "Fill";}
		});

		return true;
	}

	// :::: IClearModule
	@Override
	public void clear(BuildingImageData data) {
		final ImageWorkspace workspace = data.handle.getContext();
		BuiltSelection sel = pollSelectionMask(workspace);
		workspace.getUndoEngine().performAndStore(new MaskedImageAction(data, sel) {
			@Override
			protected void performImageAction() {
				IBuiltImageData built = workspace.buildData(builtImage);
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
		// TODO Auto-generated method stub
		
	}

	// :::: IFlipModule
	@Override
	public void flip(BuildingImageData data, boolean horizontal) {
		ImageWorkspace workspace = data.handle.getContext();
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
			undoEngine.performAndStore( new FlipAction(data, selectionEngine.getBuiltSelection(), horizontal));
		else {
			UndoableAction actions[] = new UndoableAction[2];
			actions[0] = new FlipAction(data, selectionEngine.getBuiltSelection(), horizontal);
			
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
		private FlipAction(BuildingImageData data, BuiltSelection mask, boolean horizontal) {
			super(data, mask);
			this.horizontal = horizontal;
			description = "Flip Action";
		}

		@Override
		protected void performImageAction() {
			IBuiltImageData built = builtImage.handle.getContext().buildData(builtImage);
			
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
}
