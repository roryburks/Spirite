package spirite.image_data.animation_data;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.function.Predicate;

import spirite.MUtil;
import spirite.brains.RenderEngine.TransformedHandle;
import spirite.graphics.GraphicsContext;
import spirite.graphics.awt.AWTContext;
import spirite.image_data.Animation;
import spirite.image_data.GroupTree;
import spirite.image_data.GroupTree.GroupNode;
import spirite.image_data.GroupTree.LayerNode;
import spirite.image_data.GroupTree.Node;
import spirite.image_data.ImageWorkspace.AdditionChange;
import spirite.image_data.ImageWorkspace.DeletionChange;
import spirite.image_data.ImageWorkspace.MoveChange;
import spirite.image_data.ImageWorkspace.StructureChange;
import spirite.image_data.ImageWorkspace.StructureChangeEvent;
import spirite.image_data.animation_data.FixedFrameAnimation.AnimationLayerBuilder.BuildFrame;

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

	public FixedFrameAnimation(GroupNode group) {
		layers.add( constructFromGroup(group));
		name = group.getName();
		recalculateMetrics();
	}
	
	public FixedFrameAnimation( String name) {
		this.name = name;
	}
	
	public void addBuiltLayer(AnimationLayerBuilder builder) {
		
		AnimationLayer layer = new AnimationLayer();
		layer.group = builder.group;
		for( BuildFrame frame : builder.frames) {
			layer.frames.add( layer.new Frame(frame.node, frame.length, frame.marker));
		}
		
		layers.add(layer);
		_triggerChange();
	}
	
	public static class AnimationLayerBuilder {
		private final List<BuildFrame> frames = new ArrayList<>();
		private GroupNode group;
		
		/**
		 *  Adds the described frame to the list of Frames.  
		 * 
		 * @return true if added, false if the frame is malformed or you can't
		 * add any more frames
		 */
		public boolean addFrame( Marker marker, int length, LayerNode node) {
			BuildFrame frame = new BuildFrame();

			frame.marker = marker;
			frame.length = length;
			if( marker == Marker.FRAME) {
				frame.node = node;
			}
			frames.add(frame);
			
			return true;
			
//			return false;
		}
		
		public void setGroupLink( GroupNode group) {
			this.group = group;
		}
		
		class BuildFrame {
			Marker marker;
			int length;
			LayerNode node;
		}
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
		List<TransformedHandle> drawList = getDrawListForFrame(_t);
		
		
		Graphics2D g2 = (Graphics2D)g.create();
		GraphicsContext gc = new AWTContext(g2);
		
		for( TransformedHandle renderable : drawList) {
			gc.setComposite(renderable.comp, renderable.alpha);
			renderable.handle.drawLayer( gc, renderable.trans);
		}
		g2.dispose();
		
	}
	
	public List<TransformedHandle> getDrawListForFrame( int t) {
		int met = MUtil.cycle(startFrame, endFrame, t);
		
		List<TransformedHandle> drawList = new ArrayList<>();
		
		for( AnimationLayer layer : layers) {
			if( layer.getFrames().size() == 0) continue;
			
			int start = layer.getStart();
			int end = layer.getEnd();
			int localMet = met;


			// Based on the layer timing type, determine the local frame
			//	index to use (if any)
			if( layer.asynchronous) {
				localMet = MUtil.cycle(start, end, t);
			}
			
			LayerNode node = layer.getFrame(localMet);
			
			if( node != null) {
				for( TransformedHandle tr  : node.getLayer().getDrawList()) {
					tr.trans.translate(node.getOffsetX(), node.getOffsetY());
					drawList.add( tr);
				}
			}
		}
		
		drawList.sort( new  Comparator<TransformedHandle>() {
			@Override
			public int compare(TransformedHandle o1, TransformedHandle o2) {
				return o1.depth - o2.depth;
			}
		});
		
		return drawList;
	}

	@Override
	public void interpretChange(GroupNode node, StructureChangeEvent evt) {
		
		for( int i=0; i<layers.size(); ++i) {
			AnimationLayer layer = layers.get(i);
			
			if( layer.group == node) {
				StructureChange change = evt.change;
				
				if( change instanceof AdditionChange) {
					AdditionChange addition = (AdditionChange)change;
					if( addition.parent == layer.group && addition.node instanceof LayerNode) {
						if( evt.reversed)
							layer.removeNode( (LayerNode) addition.node);
						else
							layer.addNode( (LayerNode) addition.node);
					}
				}
				else if( change instanceof DeletionChange) {
					DeletionChange deletion = (DeletionChange)change;
					
					if( deletion.parent == layer.group
							&& layer.getLayers().contains(deletion.node)) 
					{
						if( evt.reversed)
							layer.addNode( (LayerNode) deletion.node);
						else
							layer.removeNode( (LayerNode)deletion.node);
					}
				}
				else if( change instanceof MoveChange) {
					MoveChange movement = (MoveChange)change;
					
					if( movement.moveNode instanceof LayerNode) {
						if( movement.oldParent == layer.group &&
							movement.oldParent == movement.newParent) {
							layer.moveNode( (LayerNode) movement.moveNode);
						}
						else if( movement.oldParent == layer.group) {
							if( evt.reversed)
								layer.addNode( (LayerNode) movement.moveNode);
							else
								layer.removeNode((LayerNode) movement.moveNode);
						}
						else if( movement.newParent == layer.group) {
							if( evt.reversed)
								layer.removeNode( (LayerNode) movement.moveNode);
							else
								layer.addNode((LayerNode) movement.moveNode);
						}
					}
				}
			}
		}


		_triggerChange();
	}
	
	@Override
	public List<GroupNode> getGroupLinks() {
		List<GroupNode> list = new ArrayList<>(layers.size());
		
		for( AnimationLayer layer : layers) {
			if( layer.group != null) {
				list.add(layer.group);
			}
		}
		
		return list;
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
		
		private AnimationLayer() {}
		
		
		public void moveNode(LayerNode moveNode) {
			Iterator<Frame> it = frames.iterator();
			while( it.hasNext()) {
				Frame frame = it.next();
				
				if( frame.getLayerNode() == moveNode) {
					it.remove();
					addFrame(moveNode,frame);
					return;
				}
			}
		}


		public void removeNode(LayerNode node) {
			frames.removeIf( new Predicate<Frame>() {
				@Override public boolean test(Frame t) {
					return ( t.node == node);
				}
			});
		}


		public void addNode(LayerNode node) { 
			addFrame( node, new Frame(node, 1, Marker.FRAME));
		}
		private void addFrame(LayerNode node, Frame frame) { 
			Node nodeBefore = node.getPreviousNode();
			
			while( nodeBefore != null && !(nodeBefore instanceof LayerNode))
				nodeBefore = nodeBefore.getPreviousNode();
			

			for( int i=0; i<frames.size(); ++i) {
				if( frames.get(i).node == nodeBefore) {
					frames.add(i, frame);
					return;
				}
			}
				
			frames.add( frames.size()-1, frame);
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
		
		public GroupNode getGroupLink() {
			return group;
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
		
		/** A map of where each Layer exists within the Frame list*/
		private Map<LayerNode,Integer> getLayerMap() {
			Map<LayerNode,Integer> map = new HashMap<>();
			for( int i=0; i<frames.size(); ++i) {
				Frame frame = frames.get(i);
				
				if( frame.getMarker() == Marker.FRAME) {
					map.put(frame.getLayerNode(), i);
				}
			}
			return map;
		}
		
/*		public List<Integer> getKeyTimes() {
			return new ArrayList<>(keyTimes);
		}*/
	}

}
