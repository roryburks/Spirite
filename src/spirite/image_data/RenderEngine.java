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
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import spirite.MDebug.ErrorType;
import spirite.MDebug;
import spirite.brains.CacheManager;
import spirite.brains.CacheManager.CachedImage;
import spirite.brains.MasterControl;
import spirite.brains.MasterControl.MWorkspaceObserver;
import spirite.image_data.GroupTree.GroupNode;
import spirite.image_data.GroupTree.LayerNode;
import spirite.image_data.GroupTree.Node;
import spirite.image_data.GroupTree.NodeValidator;
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
	private final Map<RenderSettings,CachedImage> imageCache = new HashMap<>();
	private final CacheManager cacheManager;
	
	// Needs Master just to add it as a Workspace Observer
	public RenderEngine( MasterControl master) {
		this.cacheManager = master.getCacheManager();
		
		if( master.getCurrentWorkspace() != null) {
			ImageWorkspace ws = master.getCurrentWorkspace();
			
			if( ws != null) {
				ws.addImageObserver( this);
			}
		}
		master.addWorkspaceObserver(this);
	}
	
	/** Renders the image using the given RenderSettings, accessing it from the
	 * cache if the image has been already rendered under those settings and no
	 * relevant ImageData has changed since then. */
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
				BufferedImage imageImage = settings.image.readImage().image;

				
				cachedImage = cacheManager.createImage(settings.width, settings.height);
				
				BufferedImage image = cachedImage.access();
				
				Graphics g = image.getGraphics();
				Graphics2D g2 = (Graphics2D)g;
				g2.setRenderingHints(settings.hints);
				g2.drawImage(imageImage, 0, 0, settings.width, settings.height, null);
				g.dispose();
			}
			else {
				cachedImage = cacheManager.cacheImage(propperRender(settings));
			}

			
			imageCache.put(settings, cachedImage);
		}
		
		return cachedImage.access();
	}
	
	/** 
	 * This method will draw the group as it's "intended" to be seen,
	 * requiring extra intermediate image data to combine the layers
	 * properly.
	 */
	private BufferedImage propperRender(RenderSettings settings) {
		GroupNode root = (settings.node == null)?settings.workspace.getRootNode():settings.node;
		
		
		// Step 1: Determine amount of data needed
		int n = _getNeededImagers( settings);
		
		if( n <= 0) return null;
		
		BufferedImage images[] = new BufferedImage[n];
		for( int i=0; i<n; ++i) {
			images[i] = new BufferedImage( settings.width, settings.height, BufferedImage.TYPE_INT_ARGB);
		}
		
		
		// Step 2: Recursively draw the image
		_propperRec( root,0, settings, images);
		
		// Flush the data we only needed to build the image
		for( int i=1; i<n;++i)
			images[i].flush();
		
		
		return images[0];
	}
	
	/** Determines the number of images needed to properly render 
	 * the given RenderSettings.  This number is equal to largest Group
	 * depth of any node. */
	private int _getNeededImagers(RenderSettings settings) {
		Node root = (settings.node == null)?settings.workspace.getRootNode():settings.node;
		int n = root.getDepth();
		NodeValidator validator = new NodeValidator() {			
			@Override
			public boolean isValid(Node node) {
				return (node.isVisible() && !(node instanceof GroupNode)
						&& node.getChildren().size() == 0);
			}

			@Override
			public boolean checkChildren(Node node) {
				return (node.isVisible() && node.getAlpha() > 0);
			}
		};
		
		List<Node> list = root.getAllNodesST(validator);

		int max = 0;
		for( Node node : list) {
			int i = node.getDepth()-n;
			if( i > max) max = i;
		}
		
		return max;
	}
	
	private void _propperRec(GroupNode node, int n, RenderSettings settings, BufferedImage[] buffer) {
		if( n < 0 || n >= buffer.length) {
			MDebug.handleError(ErrorType.STRUCTURAL, this, "Error: propperRender exceeds expected image need.");
			return;
		}
		
		Graphics g = buffer[n].getGraphics();
		Graphics2D g2 = (Graphics2D)g;
		if( settings.hints != null)
			g2.setRenderingHints(settings.hints);
		
		// Go through the node's children (in reverse), drawing any visible group
		//	found recursively and drawing any Layer found plainly.
		ListIterator<Node> it = node.getChildren().listIterator(node.getChildren().size());
		while( it.hasPrevious()) {
			Node child = it.previous();
			if( child.isVisible()) {
				if( child instanceof LayerNode) {
					LayerNode layer = (LayerNode)child;
					
					_setGraphicsSettings(g, child, settings);
					g2.drawImage( layer.data.readImage().image,
							0, 0, 
							settings.width, settings.height, 
							null);
					_resetRenderSettings(g, child, settings);
				}
				else if( child instanceof GroupNode && !child.getChildren().isEmpty()) {
					if( n == buffer.length+1) {
						MDebug.handleError(ErrorType.STRUCTURAL, this, "Error: propperRender exceeds expected image need.");
						continue;
					}
					

					_propperRec((GroupNode)child, n+1, settings, buffer);

					_setGraphicsSettings(g, child,settings);
					g2.drawImage( buffer[n+1],
							0, 0, 
							settings.width, settings.height, 
							null);
					_resetRenderSettings(g, child, settings);
				}
			}
		}
		g.dispose();
	}
	
	
	private Composite cc;
	private void _setGraphicsSettings( Graphics g, Node node, RenderSettings settings) {
		final Graphics2D g2 = (Graphics2D)g;
		 cc = g2.getComposite();
		
		if( node.alpha != 1.0f) 
			g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, node.alpha));
	}
	private void _resetRenderSettings( Graphics g, Node r, RenderSettings settings) {
		((Graphics2D)g).setComposite(cc);
	}
	
	/** Checks if the given settings have a cached version. */
	private CachedImage getCachedImage( RenderSettings settings) {
		CachedImage c = imageCache.get(settings);
		
		if( c == null) 
			return null;
		
		return c;
	}
	

	/** RenderSettings define exactly what you want to be drawn and how. */
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

		public GroupNode node = null;	// If null uses the root node
		
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
				
				if( width == -1) width = image.readImage().image.getWidth();
				if( height == -1) height = image.readImage().image.getHeight();
			}
			else {
				if( width == -1) width = workspace.getWidth();
				if( height == -1) height = workspace.getHeight();
			}
		}
		
		public List<ImageData> getImagesReliedOn() {
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
		
		// !!!! Eclipse Auto-generated.  Should work as expected
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
		// Remove all caches whose renderings would have been effected by this change
		Set<Entry<RenderSettings,CachedImage>> entrySet = imageCache.entrySet();
		
		Iterator<Entry<RenderSettings,CachedImage>> it = entrySet.iterator();
	
		while( it.hasNext()) {
			Entry<RenderSettings,CachedImage> entry = it.next();
			
			RenderSettings setting = entry.getKey();
			if( setting.workspace == evt.workspace) {
				// Since structureChanges do not effect ImageData and image draws
				//	ignore settings, pass over image renderings on structureChange
				if( setting.image != null && evt.isStructureChange())
					continue;
				
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
