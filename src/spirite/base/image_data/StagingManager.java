package spirite.base.image_data;

import java.util.ArrayList;
import java.util.List;

import spirite.base.image_data.GroupTree.Node;
import spirite.base.image_data.ImageWorkspace.ImageChangeEvent;

/**
 * The StagingManager
 * @author Rory Burks
 */
public class StagingManager {
	private final ImageWorkspace context;
	
	StagingManager( ImageWorkspace context) {
		this.context = context;
	}
	

	List<Node> stageList = new ArrayList<>();
	int toggleid = 0;
	public void stageNode( Node node) {
		stageList.add(node);
		
		ImageChangeEvent evt = new ImageChangeEvent();
		evt.nodesChanged.addAll(node.getAllAncestors());
		evt.nodesChanged.add(node);
		evt.isStructureChange = true;
		context.triggerImageRefresh(evt);
		
	}
	public void unstageNode( Node node) {
		stageList.remove(node);
		ImageChangeEvent evt = new ImageChangeEvent();
		evt.nodesChanged.addAll(node.getAllAncestors());
		evt.nodesChanged.add(node);
		evt.isStructureChange = true;
		context.triggerImageRefresh(evt);
	}
	public int getNodeStage(Node node) {
		return stageList.indexOf(node);
	}
}
