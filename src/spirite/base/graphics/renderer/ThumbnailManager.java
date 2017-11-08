package spirite.base.graphics.renderer;

import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import spirite.base.brains.MasterControl;
import spirite.base.graphics.RawImage;
import spirite.base.graphics.renderer.RenderEngine.RenderSettings;
import spirite.base.graphics.renderer.sources.RenderSource;
import spirite.base.image_data.GroupTree.LayerNode;
import spirite.base.image_data.GroupTree.Node;
import spirite.base.image_data.GroupTree.NodeValidator;
import spirite.base.image_data.ImageWorkspace;
import spirite.base.image_data.ImageWorkspace.ImageChangeEvent;
import spirite.base.image_data.MediumHandle;
import spirite.hybrid.HybridHelper;
import spirite.hybrid.HybridUtil;

/** 
 * The ThumbnailManager keeps track of rendering thumbnails, cacheing them and 
 * making sure they are not redrawn too often.	Outer classes can get the 
 * thumbnails using the RenderEngine.accessThumbnail method, but if they want
 * to change soemthing about the way Thumbnails are rendered, they will have
 * to access the Manager through the getThumbnailManager method.
 */
public class ThumbnailManager {
	private final RenderEngine renderEngine;
	private final MasterControl master;
	
	int thumbWidth = 32;
	int thumbHeight = 32;
	ThumbnailManager(RenderEngine renderEngine, MasterControl master){
		this.renderEngine = renderEngine;
		this.master = master;
		workingClass = HybridHelper.getImageType();
		thumbnailAtlas.put( workingClass, thumbnailPrimaryMap);
	}

	private final Map<Class<? extends RawImage>,Map<Node,Thumbnail>> thumbnailAtlas = new HashMap<>();
	private final Map<Node,Thumbnail> thumbnailPrimaryMap = new HashMap<>();
	private Class<? extends RawImage> workingClass;
	
	public RawImage accessThumbnail( Node node) {
		Thumbnail thumb = thumbnailPrimaryMap.get(node);
		
		if( thumb == null) {
			return null;
		}
		return thumb.img;
	}
	public RawImage accessThumbnail( Node node, Class<? extends RawImage> as) {
		if( !thumbnailAtlas.containsKey(as)) 
			thumbnailAtlas.put(as, new HashMap<>());
		
		Map<Node,Thumbnail> map = thumbnailAtlas.get(as);
		
		Thumbnail thumb = map.get(node);
		if( thumb == null) {
			RawImage raw = accessThumbnail( node);
			if( raw == null)
				return null;
			
			RawImage converted = HybridUtil.convert(raw, as);
			map.put(node, new Thumbnail(converted));
			return converted;
		}
		else {
			if( !thumb.changed)
				return thumb.img;
			
			RawImage raw = accessThumbnail( node);
			if( raw == null)
				return null;
			thumb.img = HybridUtil.convert(raw,as);
			thumb.changed = false;
			
			return thumb.img;
		}
	}

	
	/** Goes through each Node and check if there is an up-to-date thumbnail
	 * of them, rendering a new one if there isn't. */
	void refreshThumbnailCache() {
		List<Node> allNodes = new ArrayList<>();
		
		for( ImageWorkspace workspace : master.getWorkspaces()) {
			allNodes.addAll(workspace.getRootNode().getAllAncestors());
		}
		
		// Remove all entries of nodes that no longer exist.
		thumbnailPrimaryMap.keySet().retainAll(allNodes);
		
		// Then go through and update all thumbnails
		for(Node node : allNodes) {
			Thumbnail thumb = thumbnailPrimaryMap.get(node);
			
			if( thumb == null) {
				thumb = new Thumbnail(renderThumbnail(node));
				thumbnailPrimaryMap.put(node, thumb);
			}
			else if( thumb.changed) {
				if( thumb.img != null)
					thumb.img.flush();
				thumb.img = renderThumbnail(node);
				thumb.changed = false;
				
				// Tell all derived thumbnail that they're out of date, but don't
				//	update them until they're needed
				Iterator<Entry<Class<? extends RawImage>,Map<Node,Thumbnail>>> itOut = thumbnailAtlas.entrySet().iterator();
				while( itOut.hasNext()) {
					Entry<Class<? extends RawImage>,Map<Node,Thumbnail>> entry = itOut.next();
					
					if(entry.getKey() == workingClass)
						continue;
					
					Map<Node,Thumbnail> map = entry.getValue();
					Thumbnail otherThumb = map.get(node);
					if( otherThumb != null)
						otherThumb.changed = true;
				}
			}
		}
	}
	
	private RawImage renderThumbnail( Node node) {

		RenderingHints newHints = new RenderingHints(
	             RenderingHints.KEY_TEXT_ANTIALIASING,
	             RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		newHints.put(RenderingHints.KEY_ALPHA_INTERPOLATION, 
				RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
		newHints.put( RenderingHints.KEY_INTERPOLATION, 
				RenderingHints.VALUE_INTERPOLATION_BICUBIC);

		RenderSource rs = this.renderEngine.getNodeRenderTarget(node);
		if( rs == null)
			return null;
		RenderSettings settings = new RenderEngine.RenderSettings(rs);
		settings.height = thumbHeight;
		settings.width = thumbWidth;
		settings.normalize();
		return rs.render(settings);
	}
	
	private class Thumbnail {
		boolean changed = false;
		RawImage img;
		Thumbnail( RawImage img) {this.img = img;}
	}

	public void imageChanged(ImageChangeEvent evt) {
		List<MediumHandle> relevantData = evt.getChangedImages();
		List<Node> changedNodes = evt.getChangedNodes();
	
		Iterator<Entry<Node,Thumbnail>> it = thumbnailPrimaryMap.entrySet().iterator();
		
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
					return node.getRender().isVisible();
				}
				@Override
				public boolean checkChildren(Node node) {
					return node.getRender().isVisible();
				}
			});
			if( !Collections.disjoint(changedNodes, nodeDependency)){
				entry.getValue().changed = true;
				continue;
			}
		}
	}
}