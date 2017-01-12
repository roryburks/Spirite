package spirite.image_data.animation_data;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import spirite.MUtil;
import spirite.image_data.GroupTree;
import spirite.image_data.GroupTree.GroupNode;
import spirite.image_data.GroupTree.LayerNode;

public class SimpleAnimation extends AbstractAnimation
{
	private List<AnimationLayer> layers = new ArrayList<>();
	private int startFrame;
	private int endFrame;
	
	public SimpleAnimation() {
	}
	
	@Override
	public float getStartFrame() {
		return 0;
	}
	@Override
	public float getEndFrame() {
		return 0;
	}
	
	public BufferedImage renderFrame( float t) {
		
		
		return null;
	}

	@Override
	public void drawFrame(Graphics g, float t) {
		int _t = (int)Math.floor(t);
		int met = MUtil.cycle(startFrame, endFrame, _t);
		
		
		for( AnimationLayer layer : layers) {
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
				g.drawImage( layer.frames.get(frame).getImageData().getData(), 0, 0, null);
			}
		}
	}
	
	public static class AnimationLayer {
		protected GroupNode group;
		protected List<LayerNode> frames = new ArrayList<>();
		protected List<Integer> keyTimes = new ArrayList<>();
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
		
		
	}
}
