package spirite.base.image_data.animation_data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;

import spirite.base.graphics.GraphicsContext;
import spirite.base.graphics.renderer.RenderEngine.TransformedHandle;
import spirite.base.image_data.Animation;
import spirite.base.image_data.AnimationManager.AnimationState;
import spirite.base.image_data.GroupTree.GroupNode;
import spirite.base.image_data.ImageWorkspace.StructureChangeEvent;
import spirite.base.image_data.layers.SpriteLayer;
import spirite.base.image_data.layers.SpriteLayer.PartStructure;

public class RigAnimation extends Animation {
	// 0 : tx
	// 1 : ty
	// 2 : sx
	// 3 : sy
	// 4 : rot
	
	//class RigAnimLayer {
	public SpriteLayer sprite;
//	}
	
	boolean interpolatorIsConstructed = false;
	
	public RigAnimation( SpriteLayer sprite, String name) {
		this.sprite = sprite;
		this.name = name;
	}
	
	
	@Override
	public void drawFrame(GraphicsContext gc, float t) {
		sprite.draw(gc);
	}

	@Override
	public List<TransformedHandle> getDrawList(float t) {
		return sprite.getDrawList();
	}

	@Override
	public List<List<TransformedHandle>> getDrawTable(float t, AnimationState state) {
		List<List<TransformedHandle>> table = new ArrayList<>(1);
		table.add(sprite.getDrawList());
		return table;
	}

	@Override
	public float getStartFrame() {
		return 0;
	}

	@Override
	public float getEndFrame() {
		return 10;
	}

	@Override
	public void importGroup(GroupNode node) {
	}

	@Override
	public List<GroupNode> getGroupLinks() {
		return null;
	}

	@Override
	public void interpretChange(GroupNode node, StructureChangeEvent evt) {
	}


	@Override public boolean isFixedFrame() {return false; }
}
