package spirite.brains;

import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.SwingUtilities;

import spirite.MDebug;
import spirite.MDebug.ErrorType;
import spirite.MUtil;
import spirite.brains.CacheManager.CachedImage;
import spirite.brains.MasterControl.MWorkspaceObserver;
import spirite.image_data.GroupTree.GroupNode;
import spirite.image_data.GroupTree.LayerNode;
import spirite.image_data.GroupTree.Node;
import spirite.image_data.GroupTree.NodeValidator;
import spirite.image_data.ImageHandle;
import spirite.image_data.ImageWorkspace;
import spirite.image_data.ImageWorkspace.BuiltImageData;
import spirite.image_data.ImageWorkspace.ImageChangeEvent;
import spirite.image_data.ImageWorkspace.MImageObserver;
import spirite.image_data.ImageWorkspace.StructureChange;
import spirite.image_data.ReferenceManager.MReferenceObserver;
import spirite.image_data.layers.Layer;

/***
 * The RenderEngine may or may not be a big and necessary component
 * in the future.  For now all it does is take the drawQueue and
 * draw it, but in the future it might buffer recently-rendered
 * iterations of images, control various rendering paramaters, etc.
 */
public class RenderEngine 
	implements MImageObserver, MWorkspaceObserver, MReferenceObserver
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
				ws.getReferenceManager().addReferenceObserve(this);
			}
		}
		master.addWorkspaceObserver(this);
	}
	@Override
	public String toString() {
		return "Rendering Engine";
	}
	
	public static abstract class Renderable {
		private int subDepth;
		public int depth;
		public abstract void draw( Graphics g);
		
		// TODO: Implement these so the RenderEngine code
		//	is not so ugly.
		// 	public abstract int getDefaultWidth();
		//	public abstract int getDefaultHeight();
		//	public List<ImageHandle> getImageDependencies();
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
			
			RenderHelper workingData = new RenderHelper();
			
			
			// Abort if it does not make sense to draw the workspace
			if( workspace == null || workspace.getWidth() <= 0 || workspace.getHeight() <= 0
					|| settings.width == 0 || settings.height == 0) 
				return null;

			if( settings.image != null) {
				// I think I need to re-think names
				cachedImage = cacheManager.createImage(settings.width, settings.height, this);
				
				BufferedImage image = cachedImage.access();
				Graphics g = image.getGraphics();
				Graphics2D g2 = (Graphics2D)g;
				
				g2.setRenderingHints(settings.hints);
				g2.scale( settings.width / (float)settings.image.getWidth(), 
						  settings.height / (float)settings.image.getHeight());
				settings.image.drawLayer(g);
				g.dispose();
			}
			else if( settings.layer != null) {
				cachedImage = cacheManager.createImage(settings.width, settings.height, this);
				

				BufferedImage image = cachedImage.access();
				Graphics g = image.getGraphics();
				Graphics2D g2 = (Graphics2D)g;
				
				g2.scale( settings.width / (float)settings.layer.getWidth(),
						settings.height / (float)settings.layer.getHeight());
				settings.layer.draw(g);
				g.dispose();
			}
			else if( settings.refRender != ReferenceRender.NULL) {
				cachedImage = cacheManager.createImage(settings.width, settings.height, this);
				
				BufferedImage image = cachedImage.access();
				Graphics2D g2 = (Graphics2D)image.getGraphics();
				
				List<Layer> layerList = (settings.refRender == ReferenceRender.FRONT)?
						settings.workspace.getReferenceManager().getFrontList():
						settings.workspace.getReferenceManager().getBackList();
				float rw = settings.width / (float)settings.workspace.getWidth();
				float rh = settings.height / (float)settings.workspace.getHeight();
				g2.scale( rw, rh);
						
				for( Layer layer : layerList ) {
					layer.draw(g2);
				}
				
				g2.dispose();
			}
			else {
				cachedImage = cacheManager.cacheImage(propperRender(settings, workingData), this);
			}

			
			imageCache.put(settings, cachedImage);
		}
		
		if( compositionImage != null) compositionImage.flush();
		compositionImage = null;
		return cachedImage.access();
	}
	
	/** 
	 * This method will draw the group as it's "intended" to be seen,
	 * requiring extra intermediate image data to combine the layers
	 * properly.
	 */
	private BufferedImage propperRender(RenderSettings settings, RenderHelper wrk) 
	{
		// Step 1: Determine amount of data needed
		int n = _getNeededImagers( settings);

		if( n <= 0) return null;
		
		BufferedImage images[] = new BufferedImage[n];
		for( int i=0; i<n; ++i) {
			images[i] = new BufferedImage( settings.width, settings.height, BufferedImage.TYPE_INT_ARGB);
		}
		
		// Step 2: Compose the Stroke and Lifted Selection data onto the 
		//	active Image so that they appear when drawn.
		BuiltImageData dataContext= settings.workspace.buildActiveData();
		if( dataContext != null) {
			if( settings.drawSelection && settings.workspace.getSelectionEngine().getLiftedImage() != null ){

				compositionImage= new BufferedImage( 
						dataContext.handle.getWidth(), dataContext.handle.getHeight(),
						BufferedImage.TYPE_INT_ARGB);
				MUtil.clearImage(compositionImage);
				compositionContext = dataContext.handle;
				
				Graphics2D g2 = (Graphics2D)compositionImage.getGraphics();
				g2.drawImage(dataContext.handle.deepAccess(), 0, 0, null);
				g2.setTransform( dataContext.getTransform());
				g2.transform(settings.workspace.getSelectionEngine().getBuiltSelection().getDrawFromTransform());
				
				g2.drawImage( settings.workspace.getSelectionEngine().getLiftedImage().access(), 0, 0, null);
				g2.dispose();
			}
			if( settings.workspace.getDrawEngine().strokeIsDrawing()) {
				if( compositionImage == null) {
					compositionImage= new BufferedImage( 
							dataContext.handle.getWidth(), dataContext.handle.getHeight(),
							BufferedImage.TYPE_INT_ARGB);
					MUtil.clearImage(compositionImage);
					compositionContext = dataContext.handle;
				}
				else {
//					MDebug.log("Probably shouldn't have lifted data and stroke engine at the same time");
				}
				
				Graphics2D g2 = (Graphics2D)compositionImage.getGraphics();
				g2.drawImage(dataContext.handle.deepAccess(), 0, 0, null);
				settings.workspace.getDrawEngine().getStrokeEngine().drawStrokeLayer(g2);
				g2.dispose();
			}
		}

		// Step 3: Recursively draw the image
		wrk.ratioW = settings.width / (float)settings.workspace.getWidth();
		wrk.ratioH = settings.height / (float)settings.workspace.getHeight();

		_propperRec( (GroupNode)settings.node, 0, settings, images, wrk);
		
		// Flush the data we only needed to build the image
		for( int i=1; i<n;++i)
			images[i].flush();
		
		return images[0];
	}
	
	private class RenderHelper {
		//public BufferedImage selLayerBI= null;
		//public BuiltImageData buildSelectedImage;
		//public BuiltSelection builtSelection;
		float ratioW, ratioH;
	}
	
	/** Determines the number of images needed to properly render 
	 * the given RenderSettings.  This number is equal to largest Group
	 * depth of any visible node. */
	private int _getNeededImagers(RenderSettings settings) {
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
		
		List<Node> list = settings.node.getAllNodesST(validator);

		int max = 0;
		for( Node node : list) {
			int i = node.getDepthFrom(settings.node);
			if( i > max) max = i;
		}
		
		return max;
	}
	
	private void _propperRec(
			GroupNode node, 
			int n, 
			RenderSettings settings, 
			BufferedImage[] buffer,
			RenderHelper wrk) 
	{
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
		
		// Step 1: Construct a list of all components that need to be rendered
		int count = 0;	// This subDepth counter is used to make sure Renderables of
						// the same depth are rendered in the correct order.
		
		
		ListIterator<Node> it = node.getChildren().listIterator(node.getChildren().size());
		List< Renderable> renderList = new ArrayList<>();
		while( it.hasPrevious()) {
			Node child = it.previous();
			if( child.isVisible()) {
				if( child instanceof GroupNode) {
					if( n == buffer.length-1) {
						// Note: the code can reach here if all the children are invisible.
						// There might be other, unintended ways for the code to reach here.
						continue;
					}
					
					Renderable renderable;
					renderable =  new GroupRenderable(
							(GroupNode) child, n, settings, buffer, wrk);
					renderable.subDepth = count++;
					renderList.add(renderable);
				}
				else {
					List<Renderable> sub = ((LayerNode)child).getLayer().getDrawList();
					
					for( Renderable subRend : sub) {
						Renderable renderable = new TransformedRenderable(
								(LayerNode) child, subRend, settings, wrk);
						renderable.subDepth = count++;
						renderList.add(renderable );
					}
				}
			}
		}
		
		// Step 2: Sort the list by depth then subdepth, increasing.
		renderList.sort( new Comparator<Renderable>() {
			@Override
			public int compare(Renderable o1, Renderable o2) {
				if( o1.depth == o2.depth)
					return o1.subDepth - o2.subDepth;
				return o1.depth - o2.depth;
			}
		});
		
		// Step 3: Draw each one (note: GroupRenderables will recursively call _propperRec
		for( Renderable renderable : renderList) {
			renderable.draw(g2);
		}
		

		g.dispose();
	}
	
	// Perhaps a bit hacky, but when ImageData is in a certain composite state,
	//	particularly when a stroke is being drawn or if lifted Image data is
	//	being moved, the RenderImage will compose all the drawn layers onto a single
	//	image which the ImageHandle's drawLayer will call.  This de-centralizes the
	//	rendering code somewhat, but saves double-creating layers or missing changes
	//	to the image data.
	private BufferedImage compositionImage;
	private ImageHandle compositionContext = null;
	public BufferedImage getCompositeLayer( ImageHandle handle) {
		if( handle.equals(compositionContext)){
			return compositionImage;
		}
		return null;
	}
	

	private class GroupRenderable extends Renderable {
		private final GroupNode node;
		private final int n;
		private final RenderSettings settings;
		private final BufferedImage[] buffer;
		private final RenderHelper wrk;
		GroupRenderable( GroupNode node, 
				int n, 
					RenderSettings settings,
				BufferedImage[] buffer,
				RenderHelper wrk ) 
		{
			this.node = node;
			this.n = n;
			this.settings = settings;
			this.buffer = buffer;
			this.wrk = wrk;
		}
		@Override
		public void draw(Graphics g) {
			_propperRec(node, n+1, settings, buffer,wrk);
			_setGraphicsSettings(g, node,settings);
			g.drawImage( buffer[n+1],
					0, 0, 
					null);
			_resetRenderSettings(g, node,settings);
		}
		
	}
	
	/** Converts a Layer-space Renderable into an Image-space Renderable. */
	private class TransformedRenderable extends Renderable {
		private final Renderable renderable;
		private final RenderHelper wrk;
		private final RenderSettings settings;
		private final LayerNode node;
		TransformedRenderable( LayerNode node, Renderable renderable, RenderSettings settings, RenderHelper wrk) {
			this.node = node;
			this.renderable = renderable;
			this.depth = renderable.depth;
			this.wrk = wrk;
			this.settings = settings;
		}
		@Override
		public void draw(Graphics g) {
			_setGraphicsSettings(g, node,settings);
			Graphics2D g2 = (Graphics2D)g;
			AffineTransform transform = g2.getTransform();
			g2.translate(node.getOffsetX(), node.getOffsetY());
			g2.scale( wrk.ratioW, wrk.ratioH);
			renderable.draw(g2);
			
			g2.setTransform(transform);
			_resetRenderSettings(g, node,settings);
		}
		
	}
	
	
	private Composite cc;
	private void _setGraphicsSettings( Graphics g, Node node, RenderSettings settings) {
		final Graphics2D g2 = (Graphics2D)g;
		 cc = g2.getComposite();
		
		if( node.getAlpha() != 1.0f) 
			g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, node.getAlpha()));
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

	public enum ReferenceRender {
		NULL, FRONT, BACK
	}

	/** RenderSettings define exactly what you want to be drawn and how. */
	public static class RenderSettings {
		
		public ImageWorkspace workspace;
		public boolean drawSelection = true;

		
		/** If null uses default hints. */
		public RenderingHints hints = null;
		
		// if -1, uses the dimensions of the Workspace
		//	(or the Image if image is non-null)
		public int width = -1;
		public int height = -1;

		// Only one of the following can be non-null.
		//	If all are null, it draws the root node
		// I don't think either wrapping them in an Interface or using a generic 
		//	Object would do here, so for now I'll just hard-code and hope this list
		//	doesn't get too long
		public Node node = null;
		public ImageHandle image = null;
		public Layer layer = null;
		public ReferenceRender refRender = ReferenceRender.NULL;
		
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
			
			if( node instanceof LayerNode) {
				layer = ((LayerNode) node).getLayer();
				node = null;
			}
			
			// Change all the many things ignored if image is null to null
			if(image != null) {
				node = null; layer = null;refRender = ReferenceRender.NULL;
				drawSelection = false;
				
				if( width == -1) width = image.getWidth();
				if( height == -1) height = image.getHeight();
			}
			else if( layer != null) {
				node = null; refRender = ReferenceRender.NULL;
				drawSelection = false;
				if( width == -1) width = layer.getWidth();
				if( height == -1) height = layer.getHeight();
			}
			else if(refRender != ReferenceRender.NULL) {
				node = null;
				if( width == -1) width = workspace.getWidth();
				if( height == -1) height = workspace.getHeight();
			}
			else {
				if( node == null) node = workspace.getRootNode();
				if( width == -1) width = workspace.getWidth();
				if( height == -1) height = workspace.getHeight();
			}
			
		}
		
		public List<ImageHandle> getImagesReliedOn() {
			if( image != null) {
				return Arrays.asList(image);
			}
			else if( layer != null) {
				return layer.getImageDependencies();
			}
			else if( refRender != ReferenceRender.NULL) {
				LinkedHashSet<ImageHandle> set = new LinkedHashSet<ImageHandle>();
				
				if( refRender == ReferenceRender.BACK) {
					for( Layer layer : workspace.getReferenceManager().getBackList()) {
						set.addAll( layer.getImageDependencies());
					}
				}
				else if( refRender == ReferenceRender.FRONT) {
					for( Layer layer : workspace.getReferenceManager().getFrontList()) {
						set.addAll( layer.getImageDependencies());
					}
				}
				return new ArrayList<>(set);
			}
			else {
				// Get a list of all layer nodes then get a list of all ImageData
				//	contained within those nodes
				List<Node> layerNodes = node.getAllNodesST( new NodeValidator() {
					@Override
					public boolean isValid(Node node) {
						return (node instanceof LayerNode);
					}
					@Override public boolean checkChildren(Node node) {return true;}
				});
				
				List<ImageHandle> list = new LinkedList<>();
				
				Iterator<Node> it = layerNodes.iterator();
				while( it.hasNext()){
					for( ImageHandle data : ((LayerNode)it.next()).getLayer().getImageDependencies()) {
						// Avoiding duplicates should make the intersection method quicker
						if( list.indexOf(data) == -1)
							list.add(data);
					}
				}
				
				return list;
			}
		}
		

		public List<Node> getNodesReliedOn() {
			List<Node> list =  new LinkedList<>();
			if( image == null) {
				if( node == null) {return list;}
				list.addAll( node.getAllNodes());
			}
			return list;
		}
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + (drawSelection ? 1231 : 1237);
			result = prime * result + height;
			result = prime * result + ((hints == null) ? 0 : hints.hashCode());
			result = prime * result + ((image == null) ? 0 : image.hashCode());
			result = prime * result + ((layer == null) ? 0 : layer.hashCode());
			result = prime * result + ((node == null) ? 0 : node.hashCode());
			result = prime * result + ((refRender == null) ? 0 : refRender.hashCode());
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
			if (layer == null) {
				if (other.layer != null)
					return false;
			} else if (!layer.equals(other.layer))
				return false;
			if (node == null) {
				if (other.node != null)
					return false;
			} else if (!node.equals(other.node))
				return false;
			if (refRender != other.refRender)
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
	
		try {
		Iterator<Entry<RenderSettings,CachedImage>> it = entrySet.iterator();


		List<ImageHandle> relevantData = evt.getChangedImages();
		List<ImageHandle> relevantDataSel = evt.getChangedImages();
		if( evt.isSelectionLayerChange() && evt.getWorkspace().getSelectionEngine().isLifted()
				&& evt.getWorkspace().buildActiveData() != null) 
		{
			relevantDataSel.add(  evt.getWorkspace().buildActiveData().handle);
		}
		
		while( it.hasNext()) {
			Entry<RenderSettings,CachedImage> entry = it.next();
			
			RenderSettings setting = entry.getKey();
			
			
			if( setting.workspace == evt.getWorkspace()) {
				
				// Since structureChanges do not effect ImageData and image draws
				//	ignore settings, pass over image renderings on structureChange
				if( setting.image != null && evt.isStructureChange())
					continue;
				
				
				// Make sure that the particular ImageData changed is
				//	used by the Cache (if not, don't remove it)
				List<ImageHandle> dataInCommon = new LinkedList<>(setting.getImagesReliedOn());
				dataInCommon.retainAll( (setting.drawSelection) ? relevantDataSel : relevantData);
				
				List<Node> nodesInCommon = evt.getChangedNodes();
				nodesInCommon.retainAll(setting.getNodesReliedOn());
				
				if( !dataInCommon.isEmpty() || !nodesInCommon.isEmpty()) {
					// Flush the visual data from memory, then remove the entry
					entry.getValue().flush();
					it.remove();
				}
			}
		}
		} catch( ConcurrentModificationException e){
			// TODO: Very Bad.  Have to figure out where the collision is coming from.
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					MDebug.log("ConcurrentModification when checking imageChange.");
					imageCache.clear();
				}
			});
		}
	}
	

	// :::: MWorkspaceObserver
	@Override	public void currentWorkspaceChanged( ImageWorkspace ws, ImageWorkspace old) {}
	@Override	public void newWorkspace( ImageWorkspace ws) {
		ws.addImageObserver( this);
		ws.getReferenceManager().addReferenceObserve(this);
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
	@Override
	public void referenceStructureChanged(boolean hard) {
		// TODO: Make this far more discriminating with a proper Event object
		//	passing workspace and whether the front/back are changed
		

		Set<Entry<RenderSettings,CachedImage>> entrySet = imageCache.entrySet();
		
		Iterator<Entry<RenderSettings,CachedImage>> it = entrySet.iterator();
	
		while( it.hasNext()) {
			Entry<RenderSettings,CachedImage> entry = it.next();
			
			RenderSettings setting = entry.getKey();
			if( setting.refRender != ReferenceRender.NULL) {
				// Flush the visual data from memory, then remove the entry
				entry.getValue().flush();
				it.remove();
			}
		}
	}
	@Override	public void toggleReference(boolean referenceMode) {}

}
