package spirite.image_data;

import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import spirite.brains.CacheManager;
import spirite.brains.CacheManager.CachedImage;
import spirite.brains.MasterControl;
import spirite.brains.MasterControl.MWorkspaceObserver;
import spirite.image_data.GroupTree.LayerNode;
import spirite.image_data.ImageWorkspace.ImageChangeEvent;
import spirite.image_data.ImageWorkspace.MImageObserver;
import spirite.image_data.ImageWorkspace.StructureChange;
import spirite.image_data.SelectionEngine.Selection;

/***
 * The RenderEngine may or may not be a big and necessary component
 * in the future.  For now all it does is take the drawQueue and
 * draw it, but in the future it might buffer recently-rendered
 * iterations of images, control various rendering paramaters, etc.
 */
public class RenderEngine 
	implements MImageObserver, MWorkspaceObserver
{
//	List<Cache> image_cache;
	Map<RenderSettings,CachedImage> imageCache = new HashMap<>();

	final CacheManager cacheManager;
	final MasterControl master;
	
	public RenderEngine( MasterControl master) {
		this.master = master;
		this.cacheManager = master.getCacheManager();
//		image_cache = new ArrayList<>();
		
		if( master.getCurrentWorkspace() != null) {
			ImageWorkspace ws = master.getCurrentWorkspace();
			
			if( ws != null) {
				ws.addImageObserver( this);
			}
		}
		master.addWorkspaceObserver(this);
	}
	
	public BufferedImage renderImage(RenderSettings settings) {
		settings.normalize();
		
		// First Attempt to draw an image from a pre-rendered cache
		CachedImage cachedImage = getCachedImage( settings);
		
		if( cachedImage == null) {
			// Otherwise get the drawing queue and draw it
			ImageWorkspace workspace = settings.workspace;
			
			// Abort if it does not make sense to draw the workspace
			if( workspace == null || workspace.getWidth() <= 0 || workspace.getHeight() <= 0
					|| settings.width == 0 || settings.height == 0) 
				return null;

			if( settings.image != null) {
				// I think I need to re-think names
				BufferedImage imageImage = settings.image.data;

				
				cachedImage = cacheManager.createImage(settings.width, settings.height);
				
				BufferedImage image = cachedImage.access();
				
				Graphics g = image.getGraphics();
				Graphics2D g2 = (Graphics2D)g;
				g2.setRenderingHints(settings.hints);
				g2.drawImage(imageImage, 0, 0, settings.width, settings.height, null);
				g.dispose();
			}
			else
				cachedImage = drawQueue(settings);

			
			imageCache.put(settings, cachedImage);
		}
		
		return cachedImage.access();
	}
	
	private CachedImage drawQueue( RenderSettings settings) {
		ImageWorkspace workspace = settings.workspace;
		SelectionEngine selectionEngine = workspace.getSelectionEngine();
		Selection selection = null;
		ImageData lifteContext = null;
		
		if( settings.drawSelection && selectionEngine.isLifted()) {
			selection = selectionEngine.getSelection();
			lifteContext = selection.getLiftedContext();
		}
		
		int width = settings.width;
		int height= settings.height;
		
		CachedImage cachedImage = cacheManager.createImage(width, height);
		
		BufferedImage image = cachedImage.access();
		Graphics g = image.getGraphics();
		Graphics2D g2 = (Graphics2D)g;


		List<LayerNode> drawing_queue = workspace.getDrawingQueue();
		
		// Go through each layer and draw it according to the settings
		for( LayerNode layer : drawing_queue) {
			ImageData layerData = layer.getImageData();
			
			if( layerData != null) {
				Composite cc = g2.getComposite();
				
				if( layer.alpha != 1.0f) 
					g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, layer.alpha));

				g.drawImage(layer.getImageData().readImage().image, 0, 0, null);
				
				if( lifteContext == layerData) {
					g.drawImage( 
							selection.getLiftedData(), 
							selectionEngine.getOffsetX(), 
							selectionEngine.getOffsetY(),
							width,
							height,
							null);
				}
				
				g2.setComposite(cc);
			}
		}
		
		return cachedImage;
	}
	
	// Looks to see if the current rendering requested is already cached; if so, just give the cache
	private CachedImage getCachedImage( RenderSettings settings) {
		CachedImage c = imageCache.get(settings);
		
		if( c == null) 
			return null;
		
		return c;
	}
	

	
	public static class RenderSettings {
		
		public ImageWorkspace workspace;
		public boolean drawSelection = true;

		/** If image is non-null, then the only thing you draw is that
		 * image.  drawSelection and node are both ignored. */
		public ImageData image = null;
		
		/** If null uses default hints. */
		public RenderingHints hints = null;
		
		// if -1, uses the dimensions of the Workspace
		//	(or the Image if image is non-null)
		public int width = -1;
		public int height = -1;

		public GroupTree.Node node = null;	// If null uses the root node
		
		/** Converts all ambiguous settings into an explicit form
		 * so that automatic hashCode and equals methods will work
		 * properly. 
		 * 
		 * Automatically called when you attempt to render with the 
		 * settings.  You probably shouldn't call it manually outside
		 * of RenderEngine since calling it before completely 
		 * constructing your settings can malform the settings, but
		 * normalizing settings can be useful (for example, .equals()
		 * only works if both settings have been normalized)*/
		public void normalize() {
			if( workspace == null) 
				throw new UnsupportedOperationException("Cannot render a null Workspace");
			
			// Change all the many things ignored if image is null to null
			if(image != null) {
				node = null;
				drawSelection = false;
				
				if( width == -1) width = image.data.getWidth();
				if( height == -1) height = image.data.getHeight();
			}
			else {
				if( width == -1) width = workspace.getWidth();
				if( height == -1) height = workspace.getHeight();
			}
		}
		
		List<ImageData> getImagesReliedOn() {
			if( image != null) {
				List<ImageData> list = new LinkedList<>();
				list.add(image);
				return list;
			}
			else {
				// TODO: Add other cases
				return workspace.imageData;
			}
		}
		
		// Eclipse Auto-generated.  Should work as expected
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + (drawSelection ? 1231 : 1237);
			result = prime * result + height;
			result = prime * result + ((hints == null) ? 0 : hints.hashCode());
			result = prime * result + ((image == null) ? 0 : image.hashCode());
			result = prime * result + ((node == null) ? 0 : node.hashCode());
			result = prime * result + width;
			result = prime * result + ((workspace == null) ? 0 : workspace.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			RenderSettings other = (RenderSettings) obj;
			if (drawSelection != other.drawSelection)
				return false;
			if (height != other.height)
				return false;
			if (hints == null) {
				if (other.hints != null)
					return false;
			} else if (!hints.equals(other.hints))
				return false;
			if (image == null) {
				if (other.image != null)
					return false;
			} else if (!image.equals(other.image))
				return false;
			if (node == null) {
				if (other.node != null)
					return false;
			} else if (!node.equals(other.node))
				return false;
			if (width != other.width)
				return false;
			if (workspace == null) {
				if (other.workspace != null)
					return false;
			} else if (!workspace.equals(other.workspace))
				return false;
			return true;
		}

	}

	// :::: MImageObserver
	@Override	public void structureChanged(StructureChange evt) {	}
	@Override
	public void imageChanged( ImageChangeEvent evt) {
		// Remove all caches whose data 
		Set<Entry<RenderSettings,CachedImage>> entrySet = imageCache.entrySet();
		
		Iterator<Entry<RenderSettings,CachedImage>> it = entrySet.iterator();
	
		while( it.hasNext()) {
			Entry<RenderSettings,CachedImage> entry = it.next();
			
			RenderSettings setting = entry.getKey();
			if( setting.workspace == evt.workspace) {
				// Make sure that the particular ImageData changed is
				//	used by the Cache (if not, don't remove it)
				List<ImageData> intersection = new LinkedList<ImageData>(evt.dataChanged);
				intersection.retainAll(setting.getImagesReliedOn());
				
				if( !intersection.isEmpty()) {				
					// Flush the visual data from memory, then remove the entry
					entry.getValue().flush();
					it.remove();
				}
			}
		}
	}
	

	// :::: MWorkspaceObserver
	@Override	public void currentWorkspaceChanged( ImageWorkspace ws, ImageWorkspace old) {}
	@Override	public void newWorkspace( ImageWorkspace ws) {
		ws.addImageObserver( this);
	}
	@Override
	public void removeWorkspace( ImageWorkspace ws) {
		// Remove all caches whose workspace is the removed workspace
		Set<Entry<RenderSettings,CachedImage>> entrySet = imageCache.entrySet();
		
		Iterator<Entry<RenderSettings,CachedImage>> it = entrySet.iterator();
	
		while( it.hasNext()) {
			Entry<RenderSettings,CachedImage> entry = it.next();
			
			RenderSettings setting = entry.getKey();
			if( setting.workspace == ws) {
				// Flush the visual data from memory, then remove the entry
				entry.getValue().flush();
				it.remove();
			}
		}
	}

}
