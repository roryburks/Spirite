package spirite.image_data;

import java.awt.Graphics;
import java.awt.image.BufferedImage;

import spirite.MDebug;
import spirite.MDebug.WarningType;
import spirite.brains.CacheManager.CachedImage;
import spirite.image_data.ImageWorkspace.ImageChangeEvent;

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
	ImageWorkspace context;
	
	int id = -1;
	boolean locked = false;	// Mostly unused for now
	
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
	public BufferedImage deepAccess() {
		if( context == null) return null;
		return context.getData(id).access();
	}
	
	public void drawLayer(Graphics g) {
		if( context == null) {
			MDebug.handleWarning(WarningType.STRUCTURAL, null, "Tried to render a context-less image.");
			return;
		}
		CachedImage ci = context.getData(id);
		
		if( ci == null) return;
		
		if( context.getRenderEngine().getCompositeLayer(this) == null) {
			g.drawImage( ci.access(), 0, 0, null);
		}
		else {
			g.drawImage( context.getRenderEngine().getCompositeLayer(this), 0, 0,  null);
		}
	}
	
	public int getID() {
		return id;
	}

	public int getWidth() {
		return context.getWidthOf(id);
	}
	public int getHeight() {
		return context.getHeightOf(id);
	}
	
	public void refresh() {
		// Construct ImageChangeEvent and send it
		ImageChangeEvent evt = new ImageChangeEvent();
		evt.workspace = context;
		evt.dataChanged.add(this);
		context.triggerImageRefresh(evt);
	}
	
	void flush() {
		context.getData(id).flush();
	}
}
