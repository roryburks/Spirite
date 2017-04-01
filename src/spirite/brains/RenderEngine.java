package spirite.brains;

import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.Timer;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;

import spirite.Globals;
import spirite.MDebug;
import spirite.MDebug.ErrorType;
import spirite.brains.CacheManager.CachedImage;
import spirite.brains.MasterControl.MWorkspaceObserver;
import spirite.gl.GLEngine;
import spirite.gl.GLEngine.ProgramType;
import spirite.gl.GLMultiRenderer;
import spirite.gl.GLMultiRenderer.GLRenderer;
import spirite.gl.GLParameters;
import spirite.image_data.GroupTree.GroupNode;
import spirite.image_data.GroupTree.LayerNode;
import spirite.image_data.GroupTree.Node;
import spirite.image_data.GroupTree.NodeValidator;
import spirite.image_data.ImageHandle;
import spirite.image_data.ImageWorkspace;
import spirite.image_data.ImageWorkspace.BuiltImageData;
import spirite.image_data.ImageWorkspace.ImageChangeEvent;
import spirite.image_data.ImageWorkspace.MImageObserver;
import spirite.image_data.ImageWorkspace.StructureChangeEvent;
import spirite.image_data.ReferenceManager.MReferenceObserver;
import spirite.image_data.ReferenceManager.Reference;
import spirite.image_data.layers.Layer;

/***
 * The RenderEngine may or may not be a big and necessary component
 * in the future.  For now all it does is take the drawQueue and
 * draw it, but in the future it might buffer recently-rendered
 * iterations of images, control various rendering paramaters, etc.
 */

/**
 * The RenderEngine executes the rendering of images to a format users will 
 * see.  In particular, the NodeRenderSource combines all of the layers of a 
 * given group (including all subgroups) using all the Node properties (alpha,
 * offset, etc) to draw the image as it's intended to be seen.
 * 
 * The RenderEngine also caches thumbnails of layers and recently-rendered 
 * images and keeps track of whether or not the images have changed since last
 * rendering to avoid re-rendering images unnecessarily.
 * 
 * @author Rory Burks
 *
 */
public class RenderEngine 
	implements MImageObserver, MWorkspaceObserver, MReferenceObserver
{
	private final Map<RenderSettings,CachedImage> imageCache = new HashMap<>();
	private final CacheManager cacheManager;
	private final Timer sweepTimer;
	private final GLEngine engine;
	
	//  ThumbnailManager needs access to MasterControl to keep track of all
	//	workspaces that exist (easier and more reliable than hooking into Observers)
	private final MasterControl master;
	
	private final static long MAX_CACHE_RESERVE = 5*60*1000;	// 5 min
	
	public RenderEngine( MasterControl master) {
		this.engine = GLEngine.getInstance();
		this.master = master;
		this.cacheManager = master.getCacheManager();
		
		master.addWorkspaceObserver(this);
		for( ImageWorkspace workspace : master.getWorkspaces()) {
			workspace.addImageObserver( this);
			workspace.getReferenceManager().addReferenceObserve(this);
		}
		
		sweepTimer = new Timer(3 * 1000 /*3 seconds*/,new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ageOutCache();
				thumbnailManager.refreshThumbnailCache();
			}
		});
		sweepTimer.start();
	}
	@Override
	public String toString() {
		return "Rendering Engine";
	}
	
	/** Clears the cache of entries which haven't been used in a long time. */
	public void ageOutCache() {

		long currentTime = System.currentTimeMillis();
		Iterator<Entry<RenderSettings,CachedImage>> it = imageCache.entrySet().iterator();
		while( it.hasNext()) {
			Entry<RenderSettings,CachedImage> entry = it.next();
			
			if( currentTime - entry.getValue().last_used > MAX_CACHE_RESERVE) {
				MDebug.log("Aged Out Cache.");
				entry.getValue().flush();
				it.remove();
			}
		}
	}
	
	// ===================
	// ===== Render Attributes
	public static class TransformedHandle {
		public int depth;
		public Composite comp = null;
		public AffineTransform trans = new AffineTransform();
		public ImageHandle handle;
	}
	
	public enum RenderMethod {
		DEFAULT("Normal", 0), 
		COLOR_CHANGE("As Color", 0xFF0000),

		LIGHTEN("Lighten", 0),
		SUBTRACT("Subtract", 0),
		MULTIPLY("Multiply",0),
		SCREEN("Screen",0),
		;
		
		public final String description;
		public final int defaultValue;
		private RenderMethod(String description, int def) {
			this.description = description;
			this.defaultValue = def;
		}
		
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
			ImageWorkspace workspace = settings.target.workspace;	// Should be impossible for target to be null
			
			// Abort if it does not make sense to draw the workspace
			if( workspace == null || workspace.getWidth() <= 0 || workspace.getHeight() <= 0
					|| settings.width == 0 || settings.height == 0) 
				return null;
			
			cachedImage = cacheManager.cacheImage(settings.target.render(settings),this);


			
			imageCache.put(settings, cachedImage);
		}
		
		if( compositionImage != null) compositionImage.flush();
		compositionImage = null;
		return cachedImage.access();
	}
	
	// ====================
	// ==== Composite Image Management
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
	private void buildCompositeLayer(ImageWorkspace workspace) {
		BuiltImageData dataContext= workspace.buildActiveData();
		if( dataContext != null) {
			BufferedImage bi = null;
			Graphics2D g2;
			if( workspace.getSelectionEngine().getLiftedImage() != null 
				||  workspace.getDrawEngine().strokeIsDrawing()) {

				bi= new BufferedImage( 
						dataContext.getWidth(), dataContext.getHeight(),
						Globals.BI_FORMAT);
				
				g2 = (Graphics2D)bi.getGraphics();
				
				g2.setTransform(dataContext.getTransform());
				dataContext.draw(g2);
				g2.setTransform(new AffineTransform());
			
				
				if( workspace.getSelectionEngine().getLiftedImage() != null ){
					g2.setTransform( dataContext.getTransform());
					g2.transform(workspace.getSelectionEngine().getDrawFromTransform());
					
					g2.drawImage( workspace.getSelectionEngine().getLiftedImage(), 0, 0, null);
					g2.dispose();
				}
				if( workspace.getDrawEngine().strokeIsDrawing()) {
					workspace.getDrawEngine().getStrokeEngine().drawStrokeLayer(g2);
					g2.dispose();
				}
				compositionContext = dataContext.handle;
				compositionImage = bi;
			}
		}
	}
	
	
	/** Checks if the given settings have a cached version. */
	private CachedImage getCachedImage( RenderSettings settings) {
		CachedImage c = imageCache.get(settings);
		
		if( c == null) 
			return null;
		
		return c;
	}

	
	// ===================
	// ==== Thumbnail Management
	
	
	public BufferedImage accessThumbnail( Node node) {
		return thumbnailManager.accessThumbnail(node);
	}
	private final ThumbnailManager thumbnailManager = new ThumbnailManager();
	public ThumbnailManager getThumbnailManager() {return thumbnailManager;}
	/** 
	 * The ThumbnailManager keeps track of rendering thumbnails, cacheing them and 
	 * making sure they are not redrawn too often.	Outer classes can get the 
	 * thumbnails using the RenderEngine.accessThumbnail method, but if they want
	 * to change soemthing about the way Thumbnails are rendered, they will have
	 * to access the Manager through the getThumbnailManager method.
	 */
	public class ThumbnailManager {
		int thumbWidth = 32;
		int thumbHeight = 32;
		private ThumbnailManager(){}

		private final Map<Node,Thumbnail> thumbnailMap = new HashMap<>();
		
		public BufferedImage accessThumbnail( Node node) {
			Thumbnail thumb = thumbnailManager.thumbnailMap.get(node);
			
			if( thumb == null) {
				return null;
			}
			return thumb.bi;
		}

		
		/** Goes through each Node and check if there is an up-to-date thumbnail
		 * of them, rendering a new one if there isn't. */
		void refreshThumbnailCache() {
			List<Node> allNodes = new ArrayList<>();
			
			for( ImageWorkspace workspace : master.getWorkspaces()) {
				allNodes.addAll(workspace.getRootNode().getAllAncestors());
			}
			
			// Remove all entries of nodes that no longer exist.
			thumbnailMap.keySet().retainAll(allNodes);
			
			// Then go through and update all thumbnails
			for(Node node : allNodes) {
				Thumbnail thumb = thumbnailMap.get(node);
				
				if( thumb == null) {
					thumb = new Thumbnail();
					thumb.bi = renderThumbnail(node);
					thumb.changed = false;
					thumbnailMap.put(node, thumb);
				}
				else if( thumb.changed) {
					if( thumb.bi != null)
						thumb.bi.flush();
					thumb.bi = renderThumbnail(node);
					thumb.changed = false;
				}
			}
		}
		
		private BufferedImage renderThumbnail( Node node) {

			RenderingHints newHints = new RenderingHints(
		             RenderingHints.KEY_TEXT_ANTIALIASING,
		             RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			newHints.put(RenderingHints.KEY_ALPHA_INTERPOLATION, 
					RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
			newHints.put( RenderingHints.KEY_INTERPOLATION, 
					RenderingHints.VALUE_INTERPOLATION_BICUBIC);

			RenderSource rs = getNodeRenderTarget(node);
			RenderSettings settings = new RenderSettings(rs);
			settings.height = thumbHeight;
			settings.width = thumbWidth;
			settings.normalize();
			return rs.render(settings);
		}
		
		private class Thumbnail {
			boolean changed = false;
			BufferedImage bi;
		}

		public void imageChanged(ImageChangeEvent evt) {
			List<ImageHandle> relevantData = evt.getChangedImages();
			List<Node> changedNodes = evt.getChangedNodes();
			
			Iterator<Entry<Node,Thumbnail>> it = thumbnailMap.entrySet().iterator();
			while( it.hasNext()) {
				Entry<Node,Thumbnail> entry = it.next();
				if( entry.getValue().changed) continue;
				if( entry.getKey().getContext() != evt.getWorkspace()) continue;
				
				// Check to see if the thumbnail contains data that was changed
				List<LayerNode> layerNodes = entry.getKey().getAllLayerNodes();
				
				for( LayerNode layerNode : layerNodes) {
					if( !Collections.disjoint(relevantData, layerNode.getLayer().getImageDependencies())) {
						entry.getValue().changed = true;
						continue;
					}
				}
				
				// Or if the thumbnail contains nodes whose structural data has been changed
				List<Node> nodeDependency = entry.getKey().getAllNodesST(new NodeValidator() {
					@Override
					public boolean isValid(Node node) {
						return node.isVisible();
					}
					@Override
					public boolean checkChildren(Node node) {
						return node.isVisible();
					}
				});
				if( !Collections.disjoint(changedNodes, nodeDependency)){
					entry.getValue().changed = true;
					continue;
				}
			}
		}
	}

	/** RenderSettings define exactly what you want to be drawn and how. */
	public static class RenderSettings {
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + (drawSelection ? 1231 : 1237);
			result = prime * result + height;
			result = prime * result + ((hints == null) ? 0 : hints.hashCode());
			result = prime * result + ((target == null) ? 0 : target.hashCode());
			result = prime * result + width;
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
			if (target == null) {
				if (other.target != null)
					return false;
			} else if (!target.equals(other.target))
				return false;
			if (width != other.width)
				return false;
			return true;
		}

		final RenderSource target ;
		public RenderSettings( RenderSource target) {
			if( target == null) 
				throw new UnsupportedOperationException("Cannot render a null Target");
			this.target = target;
		}
		
		public boolean drawSelection = true;

		
		/** If null uses default hints. */
		public RenderingHints hints = null;
		
		// if -1, uses the dimensions of the Workspace
		//	(or the Image if image is non-null)
		public int width = -1;
		public int height = -1;

		
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
			if( width < 0) width = target.getDefaultWidth();
			if( height < 0) height = target.getDefaultHeight();
		}
	}
	
	public RenderSource getDefaultRenderTarget( ImageWorkspace workspace) {
		return new NodeRenderSource(workspace.getRootNode());
	}
	public RenderSource getNodeRenderTarget( Node node) {
		if( node instanceof LayerNode) {
			return new LayerRenderSource(node.getContext(),((LayerNode) node).getLayer());
		}
		else
			return new NodeRenderSource((GroupNode) node);
	}
	
	
	// ================
	// ==== Render Sources
	/**
	 * A RenderSource corresponds to an object which can be rendered and it implements
	 * everything needed to perform a Render using certain RenderSettings.
	 *
	 * Note: It is important that subclasses overload the equals and hashCode methods
	 * of each RenderSource since the RenderEngine uses them to determine if you
	 * are rendering the same thing as something that has already been rendered.
	 * If you just go on the built-in uniqueness test and pass them through renderImage
	 * then unless you are storing the RenderSource locally yourself (which is possible
	 * and not harmful but defeats the purpose of RenderEngine), then RenderEngine 
	 * will get clogged remembering different renders of the same image.
	 */
	public static abstract class RenderSource {
		final ImageWorkspace workspace;
		RenderSource( ImageWorkspace workspace) {this.workspace = workspace;}
		public abstract int getDefaultWidth();
		public abstract int getDefaultHeight();
		public abstract List<ImageHandle> getImagesReliedOn();
		public List<Node> getNodesReliedOn() {return new ArrayList<>(0);}
		public abstract BufferedImage render( RenderSettings settings);
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
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
			RenderSource other = (RenderSource) obj;
			if (workspace == null) {
				if (other.workspace != null)
					return false;
			} else if (!workspace.equals(other.workspace))
				return false;
			return true;
		}
	}
	
	/** 
	 * This Class will draw a group as it's "intended" to be seen,
	 * requiring extra intermediate image data to combine the layers
	 * properly.
	 */
	public class NodeRenderSource extends RenderSource {
		private final GroupNode root;
		public NodeRenderSource( GroupNode node) {
			super(node.getContext());
			this.root = node;
		}
		
		@Override
		public int getDefaultWidth() {
			return workspace.getWidth();
		}
		@Override
		public int getDefaultHeight() {
			return workspace.getHeight();
		}
		@Override
		public List<Node> getNodesReliedOn() {
			List<Node> list =  new LinkedList<>();
			list.addAll( root.getAllAncestors());
			return list;
		}
		@Override
		public List<ImageHandle> getImagesReliedOn() {
			// Get a list of all layer nodes then get a list of all ImageData
			//	contained within those nodes
			List<Node> layerNodes = root.getAllNodesST( new NodeValidator() {
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
		

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((root == null) ? 0 : root.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (!super.equals(obj))
				return false;
			if (getClass() != obj.getClass())
				return false;
			NodeRenderSource other = (NodeRenderSource) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (root == null) {
				if (other.root != null)
					return false;
			} else if (!root.equals(other.root))
				return false;
			return true;
		}
		private RenderEngine getOuterType() {
			return RenderEngine.this;
		}

		@Override
		public BufferedImage render(RenderSettings settings) {
			// TODO: Add way to detect if OpenGL is not proprly loaded, and use
			//	default AWT renderer if not
			
			NodeRenderer renderer = new GLNodeRenderer(root);
//			NodeRenderer renderer = new AWTNodeRenderer(root);
			return renderer.render(settings);
		}

	}
	
	/** */
	private abstract class NodeRenderer {
		final GroupNode root;
		NodeRenderer( GroupNode root) { this.root = root;}
		public abstract BufferedImage render(RenderSettings settings);
		
		/** Determines the number of images needed to properly render 
		 * the given RenderSettings.  This number is equal to largest Group
		 * depth of any visible node. */
		int _getNeededImagers(RenderSettings settings) {
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
			for( Node ancestor : list) {
				int i = ancestor.getDepthFrom(root);
				if( i > max) max = i;
			}
			
			return max;
		}
	}

	public class AWTNodeRenderer extends NodeRenderer {
		float ratioW;
		BufferedImage buffer[];
		float ratioH;
		ImageWorkspace workspace;
		
		AWTNodeRenderer( GroupNode node) {
			super(node);
			this.workspace = node.getContext();
		}
		
		@Override
		public BufferedImage render(RenderSettings settings) {		
			try {
				// Step 1: Determine amount of data needed
				int n = _getNeededImagers( settings);
				if( n <= 0) return null;
				
				buffer = new BufferedImage[n];
				for( int i=0; i<n; ++i) {
					buffer[i] = new BufferedImage( settings.width, settings.height, Globals.BI_FORMAT);
				}
				
				// Step 2: Compose the Stroke and Lifted Selection data onto the 
				//	active Image so that they appear when drawn.
				buildCompositeLayer(workspace);
	
				// Step 3: Recursively draw the image
				ratioW = settings.width / (float)workspace.getWidth();
				ratioH = settings.height / (float)workspace.getHeight();
	
				_render_rec( root, 0, settings);
				
				// Flush the data we only needed to build the image
				for( int i=1; i<n;++i)
					buffer[i].flush();
				
				return buffer[0];
			}
			finally {buffer = null;}
		}
		
		private void _render_rec(
				GroupNode node, 
				int n, 
				RenderSettings settings) 
		{
			if( n < 0 || n >= buffer.length) {
				MDebug.handleError(ErrorType.STRUCTURAL, "Error: propperRender exceeds expected image need.");
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
			List< Drawable> renderList = new ArrayList<>();
			while( it.hasPrevious()) {
				Node child = it.previous();
				if( child.isVisible()) {
					if( child instanceof GroupNode) {
						if( n == buffer.length-1) {
							// Note: the code can reach here if all the children are invisible.
							// There might be other, unintended ways for the code to reach here.
							continue;
						}
						
						Drawable renderable;
						renderable =  new GroupRenderable(
								(GroupNode) child, n, settings);
						renderable.subDepth = count++;
						renderList.add(renderable);
					}
					else {
						List<TransformedHandle> sub = ((LayerNode)child).getLayer().getDrawList();
						
						for( TransformedHandle subRend : sub) {
							Drawable renderable = new TransformedRenderable(
									(LayerNode) child, subRend, settings);
							renderable.subDepth = count++;
							renderList.add(renderable );
						}
					}
				}
			}
			
			// Step 2: Sort the list by depth then subdepth, increasing.
			renderList.sort( new Comparator<Drawable>() {
				@Override
				public int compare(Drawable o1, Drawable o2) {
					if( o1.depth == o2.depth)
						return o1.subDepth - o2.subDepth;
					return o1.depth - o2.depth;
				}
			});
			
			// Step 3: Draw each one (note: GroupRenderables will recursively call _propperRec
			for( Drawable renderable : renderList) {
				renderable.draw(g2);
			}

			g.dispose();
		}
		
		private abstract class Drawable {
			private int subDepth;
			protected int depth;
			public abstract void draw(Graphics g);
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
		private class GroupRenderable extends Drawable {
			private final GroupNode node;
			private final int n;
			private final RenderSettings settings;
			GroupRenderable( GroupNode node, int n, RenderSettings settings) 
			{
				this.node = node;
				this.n = n;
				this.settings = settings;
			}
			@Override
			public void draw(Graphics g) {
				_render_rec(node, n+1, settings);
				_setGraphicsSettings(g, node,settings);
				g.drawImage( buffer[n+1],
						0, 0, 
						null);
				_resetRenderSettings(g, node,settings);
			}
		}
		private class TransformedRenderable extends Drawable {
			private final TransformedHandle renderable;
			private final RenderSettings settings;
			private final LayerNode node;
			private AffineTransform transform;
			TransformedRenderable( LayerNode node, TransformedHandle renderable, RenderSettings settings) {
				this.node = node;
				this.renderable = renderable;
				this.depth = renderable.depth;
				this.settings = settings;
				this.transform = renderable.trans;
				this.transform.translate(node.getOffsetX(), node.getOffsetY());
			}
			@Override
			public void draw(Graphics g) {
				_setGraphicsSettings(g, node,settings);
				Graphics2D g2 = (Graphics2D)g;
				AffineTransform transform = g2.getTransform();
				g2.scale( ratioW, ratioH);
				renderable.handle.drawLayer(g2, this.transform, renderable.comp);
				
				g2.setTransform(transform);
				_resetRenderSettings(g, node,settings);
			}
			
		}
	}
	
	/** 
	 * This updated version uses JOGL rendering algorithms instead of 
	 * AWT rendering, so that custom fragment shaders can be injected 
	 * without performace-loss.
	 * 
	 * I will attempt to explain how the shaders work in relation to each
	 * other and why they are built that way:
	 * 
	 * To begin, we are using Porter/Duff composition rules (in particular
	 * Source Over Destination), which requires using pre-multiplied pixel
	 * format.  Now it is possible 
	 * 
	 * Any time that new colors are being drawn to a GLSurface, they must
	 * be multiplied with their alpha to convert it to premultiplied form.
	 * When GLSurfaces are drawn to GLSurfaces, the data is already premultiplied
	 * so it can be drawn normally.  Finally, data must be converted to 
	 * non-premultiplied form since that is the internal format that is used.
	 * This is done using the PASS_ESCALATE shader.  All other actions are
	 * accomplished using PASS_RENDER and appropriate uComp values.
	 * (see "pass_render.frag" for appropriate values).
	 */
	public class GLNodeRenderer extends NodeRenderer {
		GLMultiRenderer glmu[];
		float ratioW;
		float ratioH;
		ImageWorkspace workspace;
		
		public GLNodeRenderer( GroupNode node) {
			super(node);
			this.workspace = node.getContext();
		}

		@Override
		public BufferedImage render(RenderSettings settings) {
			try {
				// Step 1: Determine amount of data needed
				int n = _getNeededImagers( settings);
	
				if( n <= 0) return null;
				engine.setSurfaceSize(settings.width, settings.height);
				
				// Prepare the FrameBuffers needed for rendering
				glmu = new GLMultiRenderer[n];
				for( int i=0; i<n; ++i) {
					glmu[i] = new GLMultiRenderer(
							settings.width, settings.height, engine.getGL2());
					glmu[i].init();
					glmu[i].render(new GLRenderer() {
						@Override public void render(GL gl) {
							engine.clearSurface();
						}
					});
				}
				
				// Step 2: Compose the Stroke and Lifted Selection data onto the 
				//	active Image so that they appear when drawn.
				buildCompositeLayer(workspace);
	
				// Step 3: Recursively draw the image
				ratioW = settings.width / (float)workspace.getWidth();
				ratioH = settings.height / (float)workspace.getHeight();
	
				_render_rec( root, 0, settings);

				// Step 4: Render the top-most FBO to the GLSurface and then that
				// surface onto a BufferedImage so that Swing can draw it.
				// TODO: This last step could be avoided if I used a GLPanel instead
				//	of a basic Swing Panel
				engine.setSurfaceSize(settings.width, settings.height);
				GLParameters params = new GLParameters(settings.width, settings.height);
				params.texture = new GLParameters.GLFBOTexture(glmu[0]);
		    	engine.clearSurface();
				engine.applyPassProgram(ProgramType.PASS_ESCALATE, params, null, 
						0, 0, settings.width, settings.height, true);
				
				BufferedImage bi = engine.glSurfaceToImage();

				// Flush the data we only needed to build the image
				for( int i=0; i<n;++i) {
					glmu[i].cleanup();
				}
				
				return bi;
			}
			finally {
				glmu = null;
				//buffer = null;
			}
		}
		
		private void _render_rec(GroupNode node, int n, RenderSettings settings) 
		{
			if( n < 0 || n >= glmu.length) {
				MDebug.handleError(ErrorType.STRUCTURAL, "Error: propperRender exceeds expected image need.");
				return;
			}
			
			// Go through the node's children (in reverse), drawing any visible group
			//	found recursively and drawing any Layer found plainly.
			
			// Step 1: Construct a list of all components that need to be rendered
			int count = 0;	// This subDepth counter is used to make sure Renderables of
							// the same depth are rendered in the correct order.
			
			
			ListIterator<Node> it = node.getChildren().listIterator(node.getChildren().size());
			List< Drawable> renderList = new ArrayList<>();
			while( it.hasPrevious()) {
				Node child = it.previous();
				if( child.isVisible()) {
					if( child instanceof GroupNode) {
						if( n == glmu.length-1) {
							// Note: the code can reach here if all the children are invisible.
							// There might be other, unintended ways for the code to reach here.
							continue;
						}
						
						Drawable renderable;
						renderable =  new GroupRenderable(
								(GroupNode) child, n, settings);
						renderable.subDepth = count++;
						renderList.add(renderable);
					}
					else {
						List<TransformedHandle> sub = ((LayerNode)child).getLayer().getDrawList();
						
						for( TransformedHandle subRend : sub) {
							Drawable renderable = new TransformedRenderable(
									(LayerNode) child, subRend, settings);
							renderable.subDepth = count++;
							renderList.add(renderable );
						}
					}
				}
			}
			
			// Step 2: Sort the list by depth then subdepth, increasing.
			renderList.sort( new Comparator<Drawable>() {
				@Override
				public int compare(Drawable o1, Drawable o2) {
					if( o1.depth == o2.depth)
						return o1.subDepth - o2.subDepth;
					return o1.depth - o2.depth;
				}
			});
			
			// Step 3: Draw each one (note: GroupRenderables will recursively call _propperRec
			for( Drawable renderable : renderList) {
				renderable.draw(n);
			}
		}
		
		// ==================
		// ==== Node-specific Drawing
		private void setParamsFromNode(
				Node node, GLParameters params, boolean fullPremult, float moreAlpha) 
		{
			
			int method_num = 0;
			
			boolean premult = true;
			RenderMethod method;
			int renderValue = 0;
			int stage = workspace.getStageManager().getNodeStage(node);
			
			if( stage == -1) {
				method = node.getRenderMethod();
				renderValue = node.getRenderValue();
			}
			else {
				method = RenderMethod.COLOR_CHANGE;
				switch( stage % 3) {
				case 0: renderValue = 0xFF0000;break;
				case 1: renderValue = 0x00FF00;break;
				case 2: renderValue = 0x0000FF;break;
				}
			}
			
			
			switch( method) {
			case COLOR_CHANGE:
				method_num = 1;
				break;
			case DEFAULT:
				break;
			case LIGHTEN:
				method_num = 0;
				params.setBlendModeExt(
						GL2.GL_ONE, GL2.GL_ONE, GL2.GL_FUNC_ADD,
						GL2.GL_ZERO, GL2.GL_ONE, GL2.GL_FUNC_ADD);
				break;
			case SUBTRACT:
				method_num = 0;
				params.setBlendModeExt(
						GL2.GL_ZERO, GL2.GL_ONE_MINUS_SRC_COLOR, GL2.GL_FUNC_ADD,
						GL2.GL_ZERO, GL2.GL_ONE, GL2.GL_FUNC_ADD);
				break;
			case MULTIPLY:
				method_num = 0;
				params.setBlendModeExt(GL2.GL_DST_COLOR, GL2.GL_ONE_MINUS_SRC_ALPHA, GL2.GL_FUNC_ADD,
						GL2.GL_ZERO, GL2.GL_ONE, GL2.GL_FUNC_ADD);
				break;
			case SCREEN:
				// C = (1 - (1-DestC)*(1-SrcC) = SrcC*(1-DestC) + DestC
				method_num = 0;
				params.setBlendModeExt(GL2.GL_ONE_MINUS_DST_COLOR, GL2.GL_ONE, GL2.GL_FUNC_ADD,
						GL2.GL_ZERO, GL2.GL_ONE, GL2.GL_FUNC_ADD);
				break;
			}
			params.addParam( new GLParameters.GLParam1f("uAlpha", node.getAlpha()*moreAlpha));
			params.addParam( new GLParameters.GLParam1ui("uValue", renderValue));
			params.addParam( new GLParameters.GLParam1i("uComp", (method_num << 1) | ((fullPremult&&premult)?1:0)));
		}
		
		private abstract class Drawable {
			private int subDepth;
			protected int depth;
			public abstract void draw(int ind);
		}

		private class GroupRenderable extends Drawable {
			private final GroupNode node;
			private final int n;
			private final RenderSettings settings;
			GroupRenderable( GroupNode node, int n, RenderSettings settings) 
			{
				this.node = node;
				this.n = n;
				this.settings = settings;
			}
			@Override
			public void draw(int ind) {
				glmu[n+1].render( engine.clearRenderer);
				_render_rec(node, n+1, settings);

				glmu[n].render( new GLRenderer() {
					@Override
					public void render(GL _gl) {
						GLParameters params = new GLParameters(settings.width, settings.height);
						setParamsFromNode( node, params, false, 1);
						params.texture = new GLParameters.GLFBOTexture(glmu[n+1]);
						engine.applyPassProgram(ProgramType.PASS_RENDER, params, null,
								0, 0, params.width, params.height, true);
					}
				});
			}
		}
		private class TransformedRenderable extends Drawable {
			private final TransformedHandle renderable;
			private final RenderSettings settings;
			private final LayerNode node;
			private AffineTransform transform;
			TransformedRenderable( LayerNode node, TransformedHandle renderable, RenderSettings settings) {
				this.node = node;
				this.renderable = renderable;
				this.depth = renderable.depth;
				this.settings = settings;
				this.transform = renderable.trans;
				this.transform.translate(node.getOffsetX(), node.getOffsetY());
			}
			@Override
			public void draw(int ind) {

				glmu[ind].render( new GLRenderer() {
					@Override
					public void render(GL _gl) {
						
						GLParameters params = new GLParameters(settings.width, settings.height);
						
						float alpha = 1;
						if( renderable.comp instanceof AlphaComposite)
							alpha *= ((AlphaComposite)renderable.comp).getAlpha();
						setParamsFromNode( node, params, true, alpha);
						
						AffineTransform trans = new AffineTransform(transform);

						BufferedImage bi =  getCompositeLayer(renderable.handle);
						if( bi == null) {
							bi = renderable.handle.deepAccess();
							trans.translate(renderable.handle.getDynamicX(), 
									renderable.handle.getDynamicY());
						}
						
						params.texture = new GLParameters.GLImageTexture(bi);
						engine.applyPassProgram(
								ProgramType.PASS_RENDER, params, trans,
								0, 0, bi.getWidth(), bi.getHeight(), false);
					}
				});
			}
			
		}
	}
	
	/** This renders an Image rather plainly. */
	public static class ImageRenderSource extends RenderSource {
		private final ImageHandle handle;
		public ImageRenderSource( ImageHandle handle) {
			super(handle.getContext());
			this.handle = handle;
		}
		
		@Override
		public int getDefaultWidth() {
			return handle.getWidth();
		}
		@Override
		public int getDefaultHeight() {
			return handle.getHeight();
		}

		@Override
		public List<ImageHandle> getImagesReliedOn() {
			return Arrays.asList(handle);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + ((handle == null) ? 0 : handle.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (!super.equals(obj))
				return false;
			if (getClass() != obj.getClass())
				return false;
			ImageRenderSource other = (ImageRenderSource) obj;
			if (handle == null) {
				if (other.handle != null)
					return false;
			} else if (!handle.equals(other.handle))
				return false;
			return true;
		}

		@Override
		public BufferedImage render(RenderSettings settings) {
			BufferedImage bi = new BufferedImage(
					settings.width, settings.height, Globals.BI_FORMAT);
			
			Graphics g = bi.getGraphics();
			Graphics2D g2 = (Graphics2D)g;
			
			g2.setRenderingHints(settings.hints);
			g2.scale( settings.width / (float)handle.getWidth(), 
					  settings.height / (float)handle.getHeight());
			handle.drawLayer(g,null);
			g.dispose();
			return null;
		}
	}

	
	public static class LayerRenderSource extends RenderSource {
		private final Layer layer;
		public LayerRenderSource( ImageWorkspace workspace, Layer layer) {
			super(workspace);
			this.layer = layer;
		}
		
		@Override
		public int getDefaultWidth() {
			return layer.getWidth();
		}

		@Override
		public int getDefaultHeight() {
			return layer.getHeight();
		}

		@Override
		public List<ImageHandle> getImagesReliedOn() {
			return layer.getImageDependencies();
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + ((layer == null) ? 0 : layer.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (!super.equals(obj))
				return false;
			if (getClass() != obj.getClass())
				return false;
			LayerRenderSource other = (LayerRenderSource) obj;
			if (layer == null) {
				if (other.layer != null)
					return false;
			} else if (!layer.equals(other.layer))
				return false;
			return true;
		}

		@Override
		public BufferedImage render(RenderSettings settings) {
			BufferedImage bi = new BufferedImage(
					settings.width, settings.height, Globals.BI_FORMAT);
			
			Graphics g = bi.getGraphics();
			Graphics2D g2 = (Graphics2D)g;
			
			g2.scale( settings.width / (float)layer.getWidth(),
					settings.height / (float)layer.getHeight());
			layer.draw(g);
			g.dispose();
			return bi;
		}
	}

	/** This renders the Reference section, either the front section (the part placed
	 * over the image) or the back section (the part placed behind). */
	public static class ReferenceRenderSource extends RenderSource {

		private final boolean front;
		public ReferenceRenderSource( ImageWorkspace workspace, boolean front) {
			super(workspace);
			this.front = front;
		}
		
		@Override
		public int getDefaultWidth() { return workspace.getWidth(); }
		@Override
		public int getDefaultHeight() { return workspace.getHeight(); }
		@Override
		public List<ImageHandle> getImagesReliedOn() {
			return workspace.getReferenceManager().getDependencies(front);
		}

		@Override
		public BufferedImage render(RenderSettings settings) {
			BufferedImage bi = new BufferedImage(
					settings.width, settings.height, Globals.BI_FORMAT);
			Graphics2D g2 = (Graphics2D)bi.getGraphics();
			
			List<Reference> refList = workspace.getReferenceManager().getList(front);
			float rw = settings.width / (float)workspace.getWidth();
			float rh = settings.height / (float)workspace.getHeight();
			g2.scale( rw, rh);
					
			for( Reference ref : refList ) {
				if( ref.isGlobal())
					ref.draw(g2);
			}
			
			g2.dispose();
			return bi;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + (front ? 1231 : 1237);
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (!super.equals(obj))
				return false;
			if (getClass() != obj.getClass())
				return false;
			ReferenceRenderSource other = (ReferenceRenderSource) obj;
			if (front != other.front)
				return false;
			return true;
		}
	}
	
	
	// ===================
	// ==== Implemented Interfaces

	// :::: MImageObserver
	@Override	public void structureChanged(StructureChangeEvent evt) {	}
	@Override
	public void imageChanged( ImageChangeEvent evt) {
		// Remove all caches whose renderings would have been effected by this change
		Set<Entry<RenderSettings,CachedImage>> entrySet = imageCache.entrySet();
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
			
			if( setting.target.workspace == evt.getWorkspace()) {
				// Make sure that the particular ImageData changed is
				//	used by the Cache (if not, don't remove it)
				List<ImageHandle> dataInCommon = new ArrayList<>(setting.target.getImagesReliedOn());
				dataInCommon.retainAll( (setting.drawSelection) ? relevantDataSel : relevantData);
				
				List<Node> nodesInCommon = evt.getChangedNodes();
				nodesInCommon.retainAll(setting.target.getNodesReliedOn());
				
				if( !dataInCommon.isEmpty() || !nodesInCommon.isEmpty()) {
					// Flush the visual data from memory, then remove the entry
					entry.getValue().flush();
					it.remove();
				}
			}
		}
		thumbnailManager.imageChanged(evt);
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
			if( setting.target.workspace == ws) {
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
		//
		//	Low Priority: References are changed rarely, it's tedious to determine
		//	if front/back are effected often, and if a Workspace isn't being effected
		//	by a reference change chances are it isn't drawing the reference anyway
		

		Set<Entry<RenderSettings,CachedImage>> entrySet = imageCache.entrySet();
		
		Iterator<Entry<RenderSettings,CachedImage>> it = entrySet.iterator();
	
		while( it.hasNext()) {
			Entry<RenderSettings,CachedImage> entry = it.next();
			
			RenderSettings setting = entry.getKey();
			if( setting.target instanceof ReferenceRenderSource) {
				// Flush the visual data from memory, then remove the entry
				entry.getValue().flush();
				it.remove();
			}
		}
	}
	@Override	public void toggleReference(boolean referenceMode) {}

}
