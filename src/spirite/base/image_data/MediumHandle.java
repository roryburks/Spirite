package spirite.base.image_data;


import spirite.base.graphics.GraphicsContext;
import spirite.base.graphics.GraphicsContext.Composite;
import spirite.base.graphics.IImage;
import spirite.base.image_data.ImageWorkspace.ImageChangeEvent;
import spirite.base.image_data.mediums.DynamicMedium;
import spirite.base.image_data.mediums.IMedium;
import spirite.base.image_data.mediums.PrismaticMedium;
import spirite.base.util.linear.MatTrans;
import spirite.hybrid.MDebug;
import spirite.hybrid.MDebug.WarningType;

/**
 * A MediumHandle is a reference to a Medium which is being maintained by
 * an ImageWorkspace.  All Semi-permanent Mediums (other than those in the
 * UndoEngine) should be managed through Handles and with the Workspace, 
 * otherwise their low-level Graphical resources will only be released when
 * the GC gets around to clearing them.
 * 
 * Null-context MediumHandles can be created but their intended use is for 
 * the LoadEngine which loads the logical structure then links the ImageData,
 * to avoid navigating a complex or even cyclical hierarchy of references
 * but can presumably be used in a similar fashion, but this is discouraged.
 * 
 * !!!! NOTE: Use .equals().  There should be little to no reason to ever
 * use <code>==</code> as it defeats the entire point of handles.
 */
public class MediumHandle {
	// These variables are essentially final, but may take a while to be set
	ImageWorkspace context;
	int id = -1;
	
	// ImageWorkspace.getData should have constant-time access (being implemented
	//	with a HashTable, so there's probably no point to remember the
	//	CachedImage and doing so might lead to unnecessary bugs.
	
	public MediumHandle( ImageWorkspace context, int id) {
		this.context = context;
		this.id = id;
	}
	
	/** Returns a null-context duplicate (just preserves the ID) */
	public MediumHandle dupe() {
		return new MediumHandle( null, this.id);
	}
	
	@Override
	public boolean equals(Object obj) {
		if( !( obj instanceof MediumHandle))
			return false;
		MediumHandle other = (MediumHandle)obj;
		
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
	public IImage deepAccess() {
		// TODO: BAD
		if( context == null) return null;
		return context.getData(id).readOnlyAccess();
	}


	public int getID() { return id;}
	public int getWidth() { return context.getWidthOf(id); }
	public int getHeight() { return context.getHeightOf(id); }
	public boolean isDynamic() {
		return ( context != null && context.getData(id) instanceof DynamicMedium);
	}
	public int getDynamicX() {
		if( context == null) return 0;
		IMedium ii = context.getData(id);
		return ii.getDynamicX();
	}
	public int getDynamicY() {
		if( context == null) return 0;
		IMedium ii = context.getData(id);
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
		IMedium ii = context.getData(id);
		
		if( ii == null) return;

		MatTrans prev= gc.getTransform();
		if (transform == null)
			transform = new MatTrans();
		transform.preTranslate( ii.getDynamicX(), ii.getDynamicY());
		
		MatTrans completeTransform = new MatTrans(prev);
		completeTransform.concatenate(transform);
		
		gc.setTransform(completeTransform);
		gc.drawHandle(this, 0, 0);
		gc.setTransform(prev);
	}
	
	// !!! START BAD
	public void drawBehindStroke( GraphicsContext gc) {
		if( context == null) {
			MDebug.handleWarning(WarningType.STRUCTURAL, null, "Tried to render a context-less image.");
			return;
		}
		IMedium ii = context.getData(id);
		if( ii instanceof PrismaticMedium) {
			((PrismaticMedium) ii).drawBehind(gc, context.getPaletteManager().getActiveColor(0));
		}
		else 
			gc.drawHandle(this, 0, 0);
	}
	public void drawInFrontOfStroke( GraphicsContext gc) {
		if( context == null) {
			MDebug.handleWarning(WarningType.STRUCTURAL, null, "Tried to render a context-less image.");
			return;
		}
		IMedium ii = context.getData(id);
		if( ii instanceof PrismaticMedium) {
			((PrismaticMedium) ii).drawFront(gc, context.getPaletteManager().getActiveColor(0));
		}
	}
	// !!! END BAD
		
	public void refresh() {
		// Construct ImageChangeEvent and send it
		ImageChangeEvent evt = new ImageChangeEvent();
		evt.workspace = context;
		evt.dataChanged.add(this);
		context.triggerImageRefresh(evt);
	}
}
