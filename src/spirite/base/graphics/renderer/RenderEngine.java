package spirite.base.graphics.renderer;

import spirite.base.brains.MasterControl;
import spirite.base.brains.MasterControl.MWorkspaceObserver;
import spirite.base.graphics.GraphicsContext;
import spirite.base.graphics.RawImage;
import spirite.base.graphics.renderer.sources.LayerRenderSource;
import spirite.base.graphics.renderer.sources.NodeRenderSource;
import spirite.base.graphics.renderer.sources.ReferenceRenderSource;
import spirite.base.graphics.renderer.sources.RenderSource;
import spirite.base.image_data.GroupTree.GroupNode;
import spirite.base.image_data.GroupTree.LayerNode;
import spirite.base.image_data.GroupTree.Node;
import spirite.base.image_data.ImageWorkspace;
import spirite.base.image_data.ImageWorkspace.ImageChangeEvent;
import spirite.base.image_data.ImageWorkspace.MImageObserver;
import spirite.base.image_data.ImageWorkspace.StructureChangeEvent;
import spirite.base.image_data.MediumHandle;
import spirite.base.image_data.ReferenceManager.MReferenceObserver;
import spirite.base.image_data.ReferenceManager.Reference;
import spirite.base.util.linear.MutableTransform;
import spirite.base.util.linear.Transform;
import spirite.hybrid.MDebug;

import javax.swing.Timer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;
import java.util.Map.Entry;

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
{
	private class CachedImage {
		long last_used;
		RawImage image;
		
		CachedImage( RawImage image) {
			this.image = image;
			last_used = System.currentTimeMillis();
		}
		
		RawImage access() {
			last_used = System.currentTimeMillis();
			return image;
		}
		
		void flush() {
			image.flush();
		}
	}
	
	private final Map<RenderSettings,CachedImage> imageCache = new HashMap<>();
	private final Timer sweepTimer;
	
	//  ThumbnailManager needs access to MasterControl to keep track of all
	//	workspaces that exist (easier and more reliable than hooking into Observers)
	private final MasterControl master;
	
	private final static long MAX_CACHE_RESERVE = 5*60*1000;	// 5 min
	
	public RenderEngine( MasterControl master) {
		this.master = master;
		this.thumbnailManager = new ThumbnailManager(this, master);
		//this.settingsManager = master.getSettingsManager();
		
		master.addWorkspaceObserver(workspaceObserver);
		for( ImageWorkspace workspace : master.getWorkspaces()) {
			workspace.addImageObserver( imageObserver);
			workspace.getReferenceManager().addReferenceObserve(referenceObserver);
		}
		
		// 3-second timer that checks for cache time-outs
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
	
	/** Clears the cache of entries which haven't been used in MAX_CACHE_RESERVE ms. */
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
		public float alpha = 1.0f;
		public MutableTransform trans = MutableTransform.Companion.IdentityMatrix();
		public MediumHandle handle;
		public RenderMethod method = null;
		public int renderValue = 0;
	}
	
	public enum RenderMethod {
		DEFAULT("Normal", 0), 
		COLOR_CHANGE_HUE("As Color", 0xFF0000),
		COLOR_CHANGE_FULL("As Color (fully)", 0xFF0000),

		DISOLVE("Disolve", 0b11011111_11111111),
		//DISOLVE("Disolve", 0b01110010_00101111),

		LIGHTEN("Lighten", 0),
		SUBTRACT("Subtract", 0),
		MULTIPLY("Multiply",0),
		SCREEN("Screen",0),
		OVERLAY("Overlay",0),
		;
		
		public final String description;
		public final int defaultValue;
		private RenderMethod(String description, int def) {
			this.description = description;
			this.defaultValue = def;
		}
		
	}
	
	/** Renders the Workspace to an Image in its intended form.  */
	public void renderWorkspace(ImageWorkspace workspace, GraphicsContext context, Transform trans) {
		RenderSettings settings = new RenderSettings( getDefaultRenderTarget(workspace));
		settings.normalize();
		
		GroupNode root = (workspace.getAnimationManager().getView().isUsingAnimationView()) 
				? workspace.getAnimationManager().getView().getRoot()
				: workspace.getRootNode();
		HybridNodeRenderer renderer = new HybridNodeRenderer(root);
		renderer.render(settings, context, trans);
	}
	
	/** Renders the front or back Reference Image. */
	public void renderReference( ImageWorkspace workspace, GraphicsContext context, boolean front) {
		List<Reference> refList = workspace.getReferenceManager().getList(front);
		for( Reference ref : refList )
			ref.draw(context);
	}
	
	/** Renders the image using the given RenderSettings, accessing it from the
	 * cache if the image has been already rendered under those settings and no
	 * relevant ImageData has changed since then. */
	public RawImage renderImage(RenderSettings settings) {
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
			
			cachedImage = new CachedImage(settings.target.render(settings));

			imageCache.put(settings, cachedImage);
		}
		
		return cachedImage.access();
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
	public RawImage accessThumbnail( Node node) {
		return thumbnailManager.accessThumbnail(node);
	}
	public RawImage accessThumbnail( Node node, Class<? extends RawImage> as) {
		return thumbnailManager.accessThumbnail(node, as);
	}
	private final ThumbnailManager thumbnailManager;
	public ThumbnailManager getThumbnailManager() {return thumbnailManager;}
	
	
	
	
	
	// =========
	// ==== Render Settings
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
		return new NodeRenderSource(workspace.getRootNode(), master);
	}
	public RenderSource getNodeRenderTarget( Node node) {
		if( node instanceof LayerNode) {
			return new LayerRenderSource(node.getContext(),((LayerNode) node).getLayer());
		}
		else if( node instanceof GroupNode)
			return new NodeRenderSource((GroupNode) node, master);
		
		return null;
	}
	
	// ===================
	// ==== Observers
	private final MImageObserver imageObserver = new MImageObserver() {
		@Override public void structureChanged(StructureChangeEvent evt) {}
		@Override public void imageChanged(ImageChangeEvent evt) {
			// Remove all caches whose renderings would have been effected by this change
			Set<Entry<RenderSettings,CachedImage>> entrySet = imageCache.entrySet();
			Iterator<Entry<RenderSettings,CachedImage>> it = entrySet.iterator();


			List<MediumHandle> relevantData = evt.getChangedImages();
			List<MediumHandle> relevantDataSel = evt.getChangedImages();
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
					List<MediumHandle> dataInCommon = new ArrayList<>(setting.target.getImagesReliedOn());
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
	};
	
	private final MWorkspaceObserver workspaceObserver = new MWorkspaceObserver() {
		@Override public void removeWorkspace(ImageWorkspace ws) {}
		@Override public void newWorkspace(ImageWorkspace ws) {
			ws.addImageObserver( imageObserver);
			ws.getReferenceManager().addReferenceObserve(referenceObserver);
		}
		
		@Override
		public void currentWorkspaceChanged(ImageWorkspace ws, ImageWorkspace previous) {
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
	};
	
	private final MReferenceObserver referenceObserver = new MReferenceObserver() {
		@Override public void referenceStructureChanged(boolean hard) {}
		@Override
		public void toggleReference(boolean referenceMode) {
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
	};
}
