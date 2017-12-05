package spirite.base.graphics.renderer.sources;

import spirite.base.brains.MasterControl;
import spirite.base.graphics.GraphicsContext;
import spirite.base.graphics.RawImage;
import spirite.base.graphics.RawImage.InvalidImageDimensionsExeption;
import spirite.base.graphics.renderer.HybridNodeRenderer;
import spirite.base.graphics.renderer.RenderEngine.RenderSettings;
import spirite.base.image_data.GroupTree.GroupNode;
import spirite.base.image_data.GroupTree.LayerNode;
import spirite.base.image_data.GroupTree.Node;
import spirite.base.image_data.GroupTree.NodeValidator;
import spirite.base.image_data.MediumHandle;
import spirite.hybrid.HybridHelper;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/** 
 * This Class will draw a group as it's "intended" to be seen,
 * requiring extra intermediate image data to combine the layers
 * properly.
 */
public class NodeRenderSource extends RenderSource {

	private final GroupNode root;
	//private final MasterControl master;
	
	public NodeRenderSource( GroupNode node, MasterControl master) {
		super(node.getContext());
		//this.master = master;
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
	public List<MediumHandle> getImagesReliedOn() {
		// Get a list of all layer nodes then get a list of all ImageData
		//	contained within those nodes
		List<Node> layerNodes = root.getAllNodesST( new NodeValidator() {
			@Override
			public boolean isValid(Node node) {
				return (node instanceof LayerNode);
			}
			@Override public boolean checkChildren(Node node) {return true;}
		});
		
		List<MediumHandle> list = new LinkedList<>();
		
		Iterator<Node> it = layerNodes.iterator();
		while( it.hasNext()){
			for( MediumHandle data : ((LayerNode)it.next()).getLayer().getImageDependencies()) {
				// Avoiding duplicates should make the intersection method quicker
				if( list.indexOf(data) == -1)
					list.add(data);
			}
		}
		return list;
	}
	

	@Override
	public RawImage render(RenderSettings settings) {
//		buildCompositeLayer(workspace);
		try {
			//GraphicsDrawer drawer = master.getSettingsManager().getDefaultDrawer();
			
			RawImage img = HybridHelper.createImageNonNillable(settings.width, settings.height);
			GraphicsContext gc = img.getGraphics();
			gc.clear();
			HybridNodeRenderer renderer = new HybridNodeRenderer(root);
			renderer.render(settings, gc, null);
			
			return img;
    	} catch(InvalidImageDimensionsExeption e) {
    		return HybridHelper.createNillImage();
    	}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
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
		if (root == null) {
			if (other.root != null)
				return false;
		} else if (!root.equals(other.root))
			return false;
		return true;
	}
}