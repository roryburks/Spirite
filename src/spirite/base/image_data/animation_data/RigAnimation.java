package spirite.base.image_data.animation_data;

import java.util.List;

import spirite.base.graphics.GraphicsContext;
import spirite.base.graphics.renderer.RenderEngine.TransformedHandle;
import spirite.base.image_data.Animation;
import spirite.base.image_data.AnimationManager.AnimationState;
import spirite.base.image_data.GroupTree.GroupNode;
import spirite.base.image_data.ImageWorkspace.StructureChangeEvent;

public class RigAnimation extends Animation {
	// 0 : tx
	// 1 : ty
	// 2 : sx
	// 3 : sy
	// 4 : rot
	
	@Override
	public void drawFrame(GraphicsContext gc, float t) {
		
	}

	@Override
	public List<TransformedHandle> getDrawList(float t) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<List<TransformedHandle>> getDrawTable(float t, AnimationState state) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public float getStartFrame() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public float getEndFrame() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void importGroup(GroupNode node) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public List<GroupNode> getGroupLinks() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void interpretChange(GroupNode node, StructureChangeEvent evt) {
		// TODO Auto-generated method stub
		
	}
}
