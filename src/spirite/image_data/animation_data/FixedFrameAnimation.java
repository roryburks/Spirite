package spirite.image_data.animation_data;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import javax.imageio.ImageIO;

import spirite.MUtil;
import spirite.image_data.Animation;
import spirite.image_data.GroupTree;
import spirite.image_data.GroupTree.GroupNode;
import spirite.image_data.GroupTree.LayerNode;
import spirite.image_data.layers.Layer;

/**
 * A FixedFrameAnimation 
 * @author Rory Burks
 *
 */
public class FixedFrameAnimation extends Animation
{
	private ArrayList<AnimationLayer> layers = new ArrayList<>();
	private int startFrame;
	private int endFrame;

	public FixedFrameAnimation() {
	}

	public FixedFrameAnimation(GroupNode group) {
		layers.add( constructFromGroup(group));
	}
	private AnimationLayer constructFromGroup( GroupNode group) {

		AnimationLayer layer = new AnimationLayer();
		layer.group = group;
		name = group.getName();
		
		ListIterator<GroupTree.Node> it = group.getChildren().listIterator(group.getChildren().size());
		
		while( it.hasPrevious()) {
			GroupTree.Node node = it.previous();
			
			if( node instanceof LayerNode) {
				layer.frames.add((LayerNode) node);
			}
		}
		
		for( int i = 0; i <= layer.frames.size(); ++i) {
			layer.keyTimes.add(i);
		}

		startFrame = 0;
		endFrame = layer.frames.size();
		for( AnimationLayer other : layers) {
			if( other.getStart() < startFrame)
				startFrame = other.getStart();
			if( other.getEnd() > endFrame)
				endFrame = other.getEnd();
		}
		
		return layer;
	}
	
	public void save() {
		Layer l = layers.get(0).getLayers().get(0).getLayer();
		int c = layers.get(0).getFrames().size();
		int w = l.getWidth();
		BufferedImage bi = new BufferedImage(w*c, l.getHeight(), BufferedImage.TYPE_INT_ARGB);
		
		Graphics2D g = (Graphics2D) bi.getGraphics();
		MUtil.clearImage(bi);
		g.translate(-w, 0);
		for( int i=0; i<c; ++i) {
			g.translate(w, 0);
			layers.get(0).getLayers().get(i).getLayer().draw(g);
		}
		g.dispose();
		
		try {
			ImageIO.write(bi, "png", new File("E:/test.png"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
		
		System.out.println(_t);
		
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
			}
		}
	}

	@Override
	public void interpretLink(GroupNode node) {
		boolean done = false;
		// Descending Iteration to avoid content desync on multiple hits
		for( int i=layers.size()-1; i>=0; --i) {
			if( layers.get(i).group == node) {
				layers.remove(i);
				layers.add( constructFromGroup(node));
				done = true;
			}
		}

		if( !done)
			layers.add( constructFromGroup(node));
	
		triggerChange();
	}
	
	@Override
	public void importGroup(GroupNode node) {
		layers.add( constructFromGroup(node));
		triggerChange();
	}
	
	public static class Frame {
		public final int start;
		public final int end;
		public final LayerNode node;
		Frame( LayerNode node, int start, int end) {
			this.node = node;
			this.start = start;
			this.end = end;
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
		
		
		public int getStart() {
			if( keyTimes.isEmpty()) return 0;
			return keyTimes.get(0);
		}
		public int getEnd() {
			if( keyTimes.isEmpty()) return 0;
			return keyTimes.get(keyTimes.size()-1);
			
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
		
		public List<Frame> getFrames() {
			ArrayList<Frame> list = new ArrayList<>(frames.size());
			for( int i=0; i <frames.size(); ++i) {
				list.add( new Frame( frames.get(i), keyTimes.get(i), keyTimes.get(i+1)));
			}
			
			return list;
		}

		@SuppressWarnings("unchecked")
		private List<LayerNode> getLayers() {
			return (List<LayerNode>) frames.clone();
		}
		
		@SuppressWarnings("unchecked")
		public List<Integer> getKeyTimes() {
			return (ArrayList<Integer>) keyTimes.clone();
		}
	}

}
