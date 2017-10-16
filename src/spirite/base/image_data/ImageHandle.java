package spirite.base.image_data;


import spirite.base.graphics.GraphicsContext;
import spirite.base.graphics.GraphicsContext.Composite;
import spirite.base.graphics.RawImage;
import spirite.base.graphics.gl.GLCache;
import spirite.base.graphics.gl.GLImage;
import spirite.base.graphics.gl.GLParameters;
import spirite.base.graphics.gl.GLParameters.GLTexture;
import spirite.base.image_data.ImageWorkspace.BuildingImageData;
import spirite.base.image_data.ImageWorkspace.ImageChangeEvent;
import spirite.base.image_data.images.DynamicInternalImage;
import spirite.base.image_data.images.IBuiltImageData;
import spirite.base.image_data.images.IInternalImage;
import spirite.base.util.glmath.MatTrans;
import spirite.hybrid.MDebug;
import spirite.hybrid.MDebug.WarningType;

/**
 * Under normal circumstances an ImageHandle is a logical connection to a
 * CachedImage in an ImageWorkspace.  It also helps smooth out the process
 * of building complex data structures relying on Images that should be 
 * Cached by allowing you to create ImageHandles without a null context
 * and then linking them to the ImageWorkspace using importData
 * 
 * !!!! NOTE: Use .equals().  There should be little to no reason to ever
 * use <code>==</code> as it defeats the entire point of handles.
 */
public class ImageHandle {
	// These variables are essentially final, but may take a while to be set
	ImageWorkspace context;
	int id = -1;
	
	// ImageWorkspace.getData should have constant-time access (being implemented
	//	with a HashTable, so there's probably no point to remember the
	//	CachedImage and doing so might lead to unnecessary bugs.
	
	public ImageHandle( ImageWorkspace context, int id) {
		this.context = context;
		this.id = id;
	}
	
	/** Returns a null-context duplicate (just preserves the ID) */
	public ImageHandle dupe() {
		return new ImageHandle( null, this.id);
	}
	
	@Override
	public boolean equals(Object obj) {
		if( !( obj instanceof ImageHandle))
			return false;
		ImageHandle other = (ImageHandle)obj;
		
		return (this.context == other.context) && (this.id == other.id);
	}
	
	/** Might not be necessary or belong. */
	public ImageWorkspace getContext() {
		return context;
	}
	
	/** Should only be used for reading/copying in things that need direct
	 * access to the BufferedImage.
	 * 
	 * RETURN VALUE SHOULD NEVER BE STORED LONG-TERM, if used for writing, 
	 * will not trigger proper Observers.  And probably other bad stuff 
	 * will happen if it sticks around in GC
	 *  */
	public RawImage deepAccess() {
		// TODO: BAD
		if( context == null) return null;
		return context.getData(id).readOnlyAccess();
	}
	
	/** Accesses a potentially-cached GLTexture representing the current ImageHandle*/
	public GLTexture accessGL() {
		if( context == null) return null;
		
		RawImage image = deepAccess();
		if( image instanceof GLImage)
			return new GLParameters.GLImageTexture(image);
		
		GLCache cache = context.getGLCache();
		if( cache == null) return null;
		return cache.new GLHandleTexture(this);
	}


	public int getID() { return id;}
	public int getWidth() { return context.getWidthOf(id); }
	public int getHeight() { return context.getHeightOf(id); }
	public boolean isDynamic() {
		return ( context != null && context.getData(id) instanceof DynamicInternalImage);
	}
	public int getDynamicX() {
		if( context == null) return 0;
		IInternalImage ii = context.getData(id);
		return ii.getDynamicX();
	}
	public int getDynamicY() {
		if( context == null) return 0;
		IInternalImage ii = context.getData(id);
		return ii.getDynamicY();
	}
	
	public void drawLayer(
			GraphicsContext gc, MatTrans transform, Composite composite, float alpha) 
	{
		float oldAlpha = gc.getAlpha();
		Composite oldComposite = gc.getComposite();
		
		gc.setComposite( composite, alpha*oldAlpha);
		drawLayer( gc,transform);
		gc.setComposite(oldComposite, oldAlpha);
	}
	
	public void drawLayer(GraphicsContext gc, MatTrans transform) {
		if( context == null) {
			MDebug.handleWarning(WarningType.STRUCTURAL, null, "Tried to render a context-less image.");
			return;
		}
		IInternalImage ii = context.getData(id);
		
		if( ii == null) return;

		MatTrans prev= gc.getTransform();
		if (transform == null)
			transform = new MatTrans();
		if( ii instanceof DynamicInternalImage)
			transform.preTranslate(
					((DynamicInternalImage) ii).getDynamicX(), ((DynamicInternalImage) ii).getDynamicY());
		
		MatTrans completeTransform = new MatTrans(prev);
		completeTransform.concatenate(transform);
		
		gc.setTransform(completeTransform);
		gc.drawHandle(this, 0, 0);
		gc.setTransform(prev);
	}
		
	public void refresh() {
		// Construct ImageChangeEvent and send it
		ImageChangeEvent evt = new ImageChangeEvent();
		evt.workspace = context;
		evt.dataChanged.add(this);
		context.triggerImageRefresh(evt);
	}
}
