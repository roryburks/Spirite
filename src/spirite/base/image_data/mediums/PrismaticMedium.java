package spirite.base.image_data.mediums;

import jdk.nashorn.internal.runtime.options.Option;
import org.jetbrains.annotations.NotNull;
import spirite.base.graphics.DynamicImage;
import spirite.base.graphics.GraphicsContext;
import spirite.base.graphics.GraphicsContext.Composite;
import spirite.base.graphics.IImage;
import spirite.base.graphics.RawImage;
import spirite.base.image_data.ImageWorkspace;
import spirite.base.image_data.ImageWorkspace.BuildingMediumData;
import spirite.base.image_data.mediums.drawer.DefaultImageDrawer;
import spirite.base.image_data.mediums.drawer.IImageDrawer;
import spirite.base.util.MUtil;
import spirite.base.util.linear.MatTrans;
import spirite.base.util.linear.MatTrans.NoninvertableException;
import spirite.base.util.linear.Rect;
import spirite.base.util.linear.Vec2;
import spirite.hybrid.HybridHelper;
import spirite.hybrid.HybridUtil;
import spirite.hybrid.HybridUtil.UnsupportedImageTypeException;
import spirite.hybrid.MDebug;
import spirite.hybrid.MDebug.ErrorType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/***
 * PrismaticInternalImages are a type of Internal Image that behave similarly to 
 * Dynamic images, but separate each layer by color.
 */
public class PrismaticMedium implements IMedium {
	public static class LImg {
		// NOTE: Setting all of these to public SHOULD be fine so that
		//	PrismaticInternalImages can easily be externally built and loaded in
		//	but then you have to make sure you never pass the internal LImgs
		public int color;
		public DynamicImage img;
		
		public LImg() {}
		public LImg(LImg other, boolean deepcopy) {
			color = other.color;
			img = (deepcopy)?other.img.deepCopy():other.img;
		}
		public LImg(LImg other, boolean deepcopy, boolean copyForSave_IGNORED) {
			color = other.color;
            img = (deepcopy)?other.img.deepCopy():other.img;
			//img = HybridUtil.copyForSaving(other.img);
		}
	}
	private List<LImg> layers = new ArrayList<>();
	private RawImage compositionImg;
	private boolean compIsBuilt = false;
	private Rect compRect = null;
	private final ImageWorkspace context;
	
	public PrismaticMedium( ImageWorkspace context) {
        this.context = context;
    }
	public PrismaticMedium( List<LImg> toImport,ImageWorkspace context) {
        this.context = context;
		for( LImg limg : toImport) {
			layers.add(new LImg(limg, false));
		}
	}

	@Override
	public int getWidth() {
		buildComposition();
		return compRect.width;
	}

	@Override
	public int getHeight() {
		buildComposition();
		return compRect.height;
	}

	@Override
	public int getDynamicX() {
		buildComposition();
		return compRect.x;
	}

	@Override
	public int getDynamicY() {
		buildComposition();
		return compRect.y;
	}
	private void buildComposition() {
		if( compIsBuilt)
			return;
		
		Rect r = new Rect( 0, 0, 0, 0);
		for( LImg img : layers)
			r = r.union(img.img.getXOffset(), img.img.getYOffset(), img.img.getWidth(), img.img.getHeight());
		compRect = r;
		
		if( compositionImg != null)
			compositionImg.flush();
		if( compRect == null || compRect.isEmpty()) {
			compositionImg = HybridHelper.createNillImage();
		}
		else {
			compositionImg = HybridHelper.createImage(r.width, r.height);
			GraphicsContext gc = compositionImg.getGraphics();
			for( LImg img : layers) {
				gc.drawImage(img.img.getBase(), img.img.getXOffset() - r.x, img.img.getYOffset() - r.y);
			}
		}
		
		compIsBuilt = true;
	}

	public void drawBehind( GraphicsContext gc, int color) {
		for( LImg img : layers) {
			gc.drawImage(img.img.getBase(), img.img.getXOffset() - compRect.x, img.img.getYOffset() - compRect.y);
			if( (img.color & 0xFFFFFF) == (color & 0xFFFFFF)) return;
		}
	}
	public void drawFront(GraphicsContext gc, int color) {
		boolean drawing = false;
		for( LImg img : layers) {
			if( drawing) 
				gc.drawImage(img.img.getBase(), img.img.getXOffset() - compRect.x, img.img.getYOffset() - compRect.y);
			else if( (img.color & 0xFFFFFF) == (color & 0xFFFFFF)) 
				drawing = true;
		}
		
	}

	@Override
	public BuiltMediumData build( BuildingMediumData building) {
		return new PrismaticBuiltImageData( building);
	}

	@Override
	public IMedium dupe() {
		PrismaticMedium pii = new PrismaticMedium(context);
		
		pii.layers = new ArrayList<>(this.layers.size());
		for( LImg img : this.layers) {
			pii.layers.add(new LImg(img, true));
		}
		return pii;
	}
	@Override
	public IMedium copyForSaving() {
		PrismaticMedium pii = new PrismaticMedium(context);
		
		pii.layers = new ArrayList<>(this.layers.size());
		for( LImg img : this.layers) {
			pii.layers.add(new LImg(img, true, true));
		}
		return pii;
	}
	
	public void moveLayer(int draggingFromIndex, int draggingToIndex) {
		layers.add(draggingToIndex, layers.remove(draggingFromIndex));
		compIsBuilt = false;
	}
	
	/** Note: the LImg's it returns are copies.  Changing the internal data will
	 * not change the PrismaticImage's data, but drawing to the image WILL effect
	 * the PrismaticImage. */
	public List<LImg> getColorLayers() {
		ArrayList<LImg> list = new ArrayList<LImg>(layers.size());
		
		for( LImg limg : layers) {
			list.add( new LImg(limg, false));
		}
		
		return list;
	}

	boolean flushed = false;
	@Override
	public void flush() {
		if( !flushed) {
			for( LImg img : layers)
				img.img.flush();
			if( compositionImg != null)
				compositionImg.flush();
			flushed = true;
		}
	}
	@Override
	protected void finalize() throws Throwable {
		flush();
	}

	@Override
	public IImage readOnlyAccess() {
		buildComposition();
		return compositionImg;
	}

	@Override
	public InternalImageTypes getType() {
		return InternalImageTypes.PRISMATIC;
	}

	public class PrismaticBuiltImageData extends BuiltMediumData {
	    private final int workingColor;
	    private PrismaticBuiltImageData( BuildingMediumData building) {
	        super(building);
	        workingColor = building.color;
        }

        @NotNull
        @Override
        public MatTrans getDrawTrans() {
	        return new MatTrans();
        }

        @Override
        public int getDrawWidth() {
            return 0;
        }

        @Override
        public int getDrawHeight() {
            return 0;
        }

        @NotNull
        @Override
        public MatTrans getSourceTransform() {
            return getTrans();
        }

        @Override
        public int getSourceWidth() {
            return getWidth();
        }

        @Override
        public int getSourceHeight() {
            return getHeight();
        }

        @Override
        protected void _doOnGC(@NotNull DoerOnGC doer) {
            prepareLImg().img.doOnGC(doer, getTrans());
        }

        @Override
        protected void _doOnRaw(@NotNull DoerOnRaw doer) {
	        prepareLImg().img.doOnRaw(doer, getTrans());
        }

        private LImg prepareLImg() {
            Optional<LImg> optionalLImg = layers.stream().filter((limg) -> limg.color == workingColor).findAny();
            LImg colorLayer;
            if( optionalLImg.isPresent())
                colorLayer = optionalLImg.get();
            else {
                colorLayer = new LImg();
                colorLayer.img = new DynamicImage(context, null, 0, 0);
                colorLayer.color = workingColor;
                layers.add(colorLayer);
            }
            return colorLayer;
        }
	}

	@Override public IImageDrawer getImageDrawer(BuildingMediumData building) {return new DefaultImageDrawer(this, building);}
}
