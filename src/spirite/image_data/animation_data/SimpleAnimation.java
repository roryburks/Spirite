package spirite.image_data.animation_data;

import java.awt.Graphics;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import spirite.MUtil;
import spirite.image_data.GroupTree;
import spirite.image_data.GroupTree.GroupNode;
import spirite.image_data.GroupTree.LayerNode;

public class SimpleAnimation extends AbstractAnimation
{
	private ArrayList<AnimationLayer> layers = new ArrayList<>();
	private int startFrame;
	private int endFrame;

	public SimpleAnimation() {
	}

	public SimpleAnimation(GroupNode group) {
		AnimationLayer layer = new AnimationLayer();
		layer.group = group;
		name = group.getName();
		
		for( GroupTree.Node node : group.getChildren()) {
			if( node instanceof LayerNode) {
				layer.frames.add((LayerNode) node);
			}
		}
		
		for( int i = 0; i <= layer.frames.size(); ++i) {
			layer.keyTimes.add(i);
		}
		
		startFrame = 0;
		endFrame = layer.frames.size();
		
		layers.add(layer);
	}
	
	@Override
	public float getStartFrame() {
		return startFrame;
	}
	@Override
	public float getEndFrame() {
		return endFrame;
	}
	
	@SuppressWarnings("unchecked")
	public List<AnimationLayer> getLayers() {
		return (List<AnimationLayer>) layers.clone();
	}

	@Override
	public void drawFrame(Graphics g, float t) {
		int _t = (int)Math.floor(t);
		int met = MUtil.cycle(startFrame, endFrame, _t);
		
		
		for( AnimationLayer layer : layers) {
			if( layer.getFrames().size() == 0) continue;
			
			int start = layer.keyTimes.get(0);
			int end = layer.keyTimes.get(layer.keyTimes.size()-1);
			int frame = 0;
			int localMet = met;


			
			// Based on the layer timing type, determine the local frame
			//	index to use (if any)
			if( layer.asynchronous) {
				localMet = MUtil.cycle(start, end, _t);
				
			}
			else if( met < start) {
				continue;
			}
			else if( met >= end) {
				if( !layer.loops)
					continue;
				
				localMet = MUtil.cycle( start, end-start, met-start);
			}
			
			// Iterate through the keyTimes list until you have found the one
			//	after yours, then step back
			Iterator<Integer> it = layer.keyTimes.iterator();
			
			while( it.hasNext()) {
				int i = it.next();
				if( i > localMet)
					break;
				
				frame++;
			}
			frame--;
			
			if( frame != -1) {
				layer.frames.get(frame).getLayer().draw(g);
//				g.drawImage( layer.frames.get(frame).getImageData().readImage().image, 0, 0, null);
			}
		}
	}
	
	public static class AnimationLayer {
		protected GroupNode group;
		protected ArrayList<LayerNode> frames = new ArrayList<>();
		protected ArrayList<Integer> keyTimes = new ArrayList<>();
		protected boolean asynchronous = false;
		protected boolean loops = true;
		
		public AnimationLayer() {
		}
		
		
		
		public boolean isAsynchronous() {
			return asynchronous;
		}
		public void setAsynchronous(boolean asynchronous) {
			this.asynchronous = asynchronous;
		}
		public boolean isLoops() {
			return loops;
		}
		public void setLoops(boolean loops) {
			this.loops = loops;
		}
		
		@SuppressWarnings("unchecked")
		public List<LayerNode> getFrames() {
			return (ArrayList<LayerNode>) frames.clone();
		}
		@SuppressWarnings("unchecked")
		public List<Integer> getKeyTimes() {
			return (ArrayList<Integer>) keyTimes.clone();
		}
		
	}
}
