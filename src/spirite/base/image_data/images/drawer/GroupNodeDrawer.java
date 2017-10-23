package spirite.base.image_data.images.drawer;
import spirite.base.image_data.GroupTree.GroupNode;
import spirite.base.image_data.GroupTree.Node;
import spirite.base.image_data.ImageWorkspace;
import spirite.base.image_data.UndoEngine;
// Bad, but auto-complete include isn't working with my Eclipse
import spirite.base.image_data.images.drawer.IImageDrawer.ITransformModule;
import spirite.base.util.glmath.MatTrans;

public class GroupNodeDrawer 
	implements	IImageDrawer,
				ITransformModule
{
	private final GroupNode group;
	
	public GroupNodeDrawer( GroupNode group) {
		this.group = group;
	}

	@Override
	public void transform(MatTrans trans) {
		ImageWorkspace workspace = group.getContext();
		UndoEngine undoEngine = workspace.getUndoEngine();
		
		undoEngine.pause();
		
		for( Node child : group.getChildren()) {
			IImageDrawer drawer = workspace.getDrawerFromNode(child);
			if( drawer instanceof ITransformModule)
				((ITransformModule) drawer).transform(trans);
		}
		
		undoEngine.unpause("Batch Transform");
	}

}
