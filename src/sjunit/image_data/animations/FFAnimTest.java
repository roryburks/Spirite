package sjunit.image_data.animations;

import org.junit.Test;

import sjunit.TestWrapper;
import spirite.base.brains.MasterControl;
import spirite.base.image_data.GroupTree.GroupNode;
import spirite.base.image_data.GroupTree.LayerNode;
import spirite.base.image_data.ImageWorkspace;
import spirite.base.image_data.animations.FixedFrameAnimation;
import spirite.base.image_data.mediums.IMedium.InternalImageTypes;
import spirite.hybrid.HybridHelper;

public class FFAnimTest {

	@Test
	public void Test1() throws Exception {
		TestWrapper.performTest((MasterControl master) -> {
			ImageWorkspace ws = new ImageWorkspace(master);
			ws.finishBuilding();
//
//			GroupNode animRoot = ws.addGroupNode(ws.getRootNode(), "AnimRoot");
//
//			FixedFrameAnimation ffa = new FixedFrameAnimation(animRoot, "FFA", true);
//			ws.getAnimationManager().addAnimation(ffa);
//
//			LayerNode layer1 = ws.addNewSimpleLayer(animRoot, HybridHelper.createImage(1, 1), "1", InternalImageTypes.DYNAMIC);
//			assert( ffa.getLayers().get(0).getFrameForMet(0).getLinkedNode() == layer1);
//			LayerNode layer2 = ws.addNewSimpleLayer(animRoot, HybridHelper.createImage(1, 1), "2", InternalImageTypes.DYNAMIC);
//
//			assert( animRoot.getChildren().get(1) == layer2);
//			assert( ffa.getLayers().get(0).getFrameForMet(0).getLinkedNode() == layer1);

		});
	}

}
