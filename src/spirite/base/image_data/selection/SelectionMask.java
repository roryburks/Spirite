package spirite.base.image_data.selection;

import spirite.base.graphics.GraphicsContext;
import spirite.base.graphics.RawImage;
import spirite.base.graphics.GraphicsContext.Composite;
import spirite.base.graphics.IImage;
import spirite.base.image_data.mediums.ABuiltMediumData;
import spirite.base.image_data.selection.SelectionMask.LiftScheme;
import spirite.base.util.Colors;
import spirite.base.util.MUtil;
import spirite.base.util.glmath.Rect;
import spirite.base.util.glmath.Vec2i;
import spirite.hybrid.HybridHelper;
import spirite.hybrid.HybridUtil;
import spirite.hybrid.HybridUtil.UnsupportedImageTypeException;

/**
 * 
 * NOTE: SelectionMask is immutable.  Technically the mask is mutable, but only if
 * the IImage contract is breached.
 */
public class SelectionMask {
	final IImage mask;
	final int ox, oy;

	// ===========
	// ==== Constructors
	public SelectionMask( IImage mask) {
		this(mask,0,0);
	}
	public SelectionMask( IImage mask, int x, int y) {
		Rect cropped = null;
		try {
			cropped = HybridUtil.findContentBounds(mask, 1, true);
		} catch (UnsupportedImageTypeException e) {
			e.printStackTrace();
		}
		
		
		RawImage maskBeingBuilt = HybridHelper.createImage(cropped.width, cropped.height);
		GraphicsContext gc = maskBeingBuilt.getGraphics();
		gc.drawImage(mask, -cropped.x, -cropped.y);
		
		this.mask = maskBeingBuilt;
		this.ox = cropped.x + x;
		this.oy = cropped.y + y;
	}
	SelectionMask( SelectionMask other, int ox, int oy) {
		this.mask = other.mask;
		this.ox = ox;
		this.oy = oy;
	}

	// ==========
	// ==== Gets
	public int getOX() {return ox;}
	public int getOY() {return oy;}
	public int getWidth() {return mask.getWidth();}
	public int getHeight() {return mask.getHeight();}
	public Vec2i getDimension() {
		return new Vec2i(mask.getWidth(), mask.getHeight());
	}
	
	// ============
	// ==== Common Functionality
	public void drawMask( GraphicsContext gc, boolean withOffset) {
		if( withOffset)
			gc.drawImage(mask, ox, oy);
		else
			gc.drawImage(mask, 0, 0);
	}

	public boolean contains(int x, int y) {
		if( x < ox || x > ox + mask.getWidth() || y < oy || y > oy + mask.getHeight())
			return false;
		return Colors.getAlpha(mask.getRGB(x-ox, y-oy)) != 0;
	}


	public void drawBounds(GraphicsContext gc, boolean withOffset) {
		if( withOffset) {
			gc.pushTransform();
			gc.preTranslate(ox, oy);
			gc.drawBounds(mask, 0);
			gc.popTransform();
		}
		else
			gc.drawBounds(mask, 0);
	}

	// ===========
	// ==== Lifting
	public RawImage liftRawImage( IImage image, int ox, int oy) {
		return _liftFromScheme(new LiftScheme() {
			@Override
			public void draw(GraphicsContext gc) {
				gc.drawImage(image, ox, oy);
			}

			@Override
			public Rect getBounds() {
				return new Rect(ox,oy,image.getWidth(),image.getHeight());
			}
			
		});
	}
	public RawImage liftSelectionFromData(ABuiltMediumData built) {
		return _liftFromScheme(new LiftScheme() {
			@Override
			public void draw(GraphicsContext gc) {
				built.doOnRaw((raw) -> {
					gc.pushTransform();
					gc.transform(built.getCompositeTransform());
					gc.drawImage(raw, 0, 0);
					gc.popTransform();
				});
			}

			@Override
			public Rect getBounds() {
				return MUtil.circumscribeTrans(new Rect(0,0,built.getWidth(),built.getHeight()), built.getCompositeTransform());
			}
		});
	}
	
	private RawImage _liftFromScheme( LiftScheme liftScheme) {
		Rect selectionRect = new Rect(ox, oy, mask.getWidth(), mask.getHeight());
		
		RawImage img = HybridHelper.createImage(selectionRect.width, selectionRect.height);
		GraphicsContext gc = img.getGraphics();

		// Draw the mask, clipping the bounds of drawing to only the part 
		// that the selection	intersects with the Image so that you do not 
		//  leave un-applied mask left in the image.
		Rect dataRect = liftScheme.getBounds();
		Rect intersection = dataRect.intersection(selectionRect);
		
		gc.setClip(
				intersection.x - selectionRect.x, intersection.y - selectionRect.y, 
				intersection.width, intersection.height);
		gc.drawImage(mask, 0, 0);
		

		// Copy the data inside the Selection's alphaMask to liftedData
		gc.setComposite( Composite.SRC_IN, 1.0f);

		gc.translate(-ox, -oy);
		
		liftScheme.draw(gc);
		
		return img;
	}
	
	/** Helper Class to reduce duplicate code. */
	public interface LiftScheme {
		void draw(GraphicsContext gc);
		Rect getBounds();
	}



	
}
