package spirite.image_data.animation_data;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;

import javax.imageio.ImageIO;

import spirite.MUtil;
import spirite.brains.RenderEngine.Renderable;
import spirite.image_data.Animation;
import spirite.image_data.GroupTree;
import spirite.image_data.GroupTree.GroupNode;
import spirite.image_data.GroupTree.LayerNode;
import spirite.image_data.animation_data.FixedFrameAnimation.AnimationLayer.Frame;

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

//	public FixedFrameAnimation() {
//	}

	public FixedFrameAnimation(GroupNode group) {
		layers.add( constructFromGroup(group));
		name = group.getName();
		recalculateMetrics();
	}
	private AnimationLayer constructFromGroup( GroupNode group) {

		AnimationLayer layer = new AnimationLayer();
		layer.group = group;
		name = group.getName();
		
		ListIterator<GroupTree.Node> it = group.getChildren().listIterator(group.getChildren().size());
		
		int met = 0;
		while( it.hasPrevious()) {
			GroupTree.Node node = it.previous();
			
			if( node instanceof LayerNode) {
				layer.frames.add( layer.new Frame((LayerNode) node, 1, Marker.FRAME));
				met += 1;
			}
		}
		
		layer.frames.add(layer.new Frame(null, 0, Marker.END_AND_LOOP));
		
		return layer;
	}
	
	private void _triggerChange() {
		recalculateMetrics();
		triggerChange();
	}
	private void recalculateMetrics() {
		startFrame = 0;
		endFrame = 0;
		for( AnimationLayer layer : layers) {
			if( layer.getStart() < startFrame) startFrame = layer.getStart();
			if( layer.getEnd() >= endFrame)endFrame = layer.getEnd()+1;
		}
	}
	
	public void save() {
		int width = 0;
		int height = 0;


		for( AnimationLayer layer : layers) {
			for( Frame frame : layer.getFrames()) {
				if( frame.marker == Marker.FRAME) {
					width = Math.max( width, frame.node.getLayer().getWidth());
					height = Math.max( height, frame.node.getLayer().getHeight());
				}
			}
		}
		
		int c = (int)Math.floor(getEndFrame());
		
		BufferedImage bi = new BufferedImage(width*c, height, BufferedImage.TYPE_INT_ARGB);
		
		Graphics2D g = (Graphics2D) bi.getGraphics();
		MUtil.clearImage(bi);
		g.translate(-width, 0);
		for( int i=0; i<c; ++i) {
			g.translate(width, 0);
			drawFrame(g, i);
//			if( layers.get(0))
//			layers.get(0).getLayers().get(i).getLayer().draw(g);
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
	public int getStart() {
		return startFrame;
	}
	@Override
	public float getEndFrame() {
		return endFrame-1;
	}
	public int getEnd() {
		return endFrame-1;
	}
	
	@SuppressWarnings("unchecked")
	public List<AnimationLayer> getLayers() {
		return (List<AnimationLayer>) layers.clone();
	}

	@Override
	public void drawFrame(Graphics g, float t) {
		int _t = (int)Math.floor(t);
		int met = MUtil.cycle(startFrame, endFrame, _t);
		
		List<Renderable> drawList = new ArrayList<>();
		
		for( AnimationLayer layer : layers) {
			if( layer.getFrames().size() == 0) continue;
			
			int start = layer.getStart();
			int end = layer.getEnd();
			int frame = 0;
			int localMet = met;


			// Based on the layer timing type, determine the local frame
			//	index to use (if any)
			if( layer.asynchronous) {
				localMet = MUtil.cycle(start, end, _t);
			}
			
			LayerNode node = layer.getFrame(localMet);
			
			if( node != null) {
				drawList.addAll( node.getLayer().getDrawList());
			}
		}
		
		drawList.sort( new  Comparator<Renderable>() {
			@Override
			public int compare(Renderable o1, Renderable o2) {
				return o1.depth - o2.depth;
			}
		});
		
		for( Renderable renderable : drawList) {
			renderable.draw(g);
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


		_triggerChange();
	}
	
	
	@Override
	public void importGroup(GroupNode node) {
		layers.add( constructFromGroup(node));
		_triggerChange();
	}
	
	
	
	public enum Marker {
		FRAME,
		START_LOCAL_LOOP,	
		END_LOCAL_LOOP,
		END_AND_LOOP,
		NIL_OUT,
	}

	public class AnimationLayer {
		public class Frame {
				private int length;
				private LayerNode node;
				private Marker marker;
				private Frame( LayerNode node, int length, Marker marker) {
					this.node = node;
					this.length = length;
					this.marker = marker;
				}
				public int getStart() { 
					int i = 0;
					for( Frame frame : frames) {
						if( frame == this)
							return i;
						i += frame.length;
					}
					return Integer.MIN_VALUE; 
				}
				public int getEnd() { return getStart()+length; }
				public int getLength() {return length;}
				public LayerNode getLayerNode() { return node; }
				public Marker getMarker() { return marker;}
				
				public void setLength( int newLength) {
					if( newLength < 0) 
						throw new IndexOutOfBoundsException();
					length = newLength;
					
					_triggerChange();
				}
			}

		protected GroupNode group;
		protected final ArrayList<Frame> frames = new ArrayList<>();
		protected boolean asynchronous = false;
		
		public AnimationLayer() {
		}
		
		
		public int getStart() {
			int i = 0;
			for( Frame frame : frames) {
				if( frame.marker == Marker.FRAME) {
					return i;
				}
				i += frame.length;
			}
			return -1;
		}
		public int getEnd() {
			int i = 0;
			for( Frame frame : frames) {
				i += frame.length;
				
			}
			return i;
		}
		
		public LayerNode getFrame( int met) {
			int tick = 0;
			boolean looping = false;
			int startLoop = 0;
			int loopSize;
			LayerNode layer = null;
			List<LayerNode> loopLayer = new ArrayList<>(0);
			List<Integer> loopmarkers = new ArrayList<>(0);
			
			
			for( int index=0; index < frames.size(); ++index) {
				Frame frame = frames.get(index);

				tick += frame.length;
				switch( frame.marker) {
				case FRAME:
					layer = frame.node;
					break;
				case START_LOCAL_LOOP:
					looping = true;
					startLoop = tick;
					break;
				case END_AND_LOOP:
					if( tick == 0)
						return null;
					index = -1;
					break;
				case END_LOCAL_LOOP:
					looping = false;
					loopSize = tick - startLoop;
					break;
				case NIL_OUT:
					layer = null;
					break;
				}
				if( tick > met) {
					if( looping) {
						// TODO
					}
					else return layer;
				}
			}
			
			return layer;
		}
		
		
		public boolean isAsynchronous() {
			return asynchronous;
		}
		public void setAsynchronous(boolean asynchronous) {
			this.asynchronous = asynchronous;
		}
		
		public List<Frame> getFrames() {
			return new ArrayList<>(frames);
		}

		private List<LayerNode> getLayers() {
			ArrayList<LayerNode> list = new ArrayList<>();
			for( Frame frame : frames) {
				if( frame.marker == Marker.FRAME)
					list.add(frame.node);
			}
			return list;
		}
		
/*		public List<Integer> getKeyTimes() {
			return new ArrayList<>(keyTimes);
		}*/
	}

}
