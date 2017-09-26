package spirite.base.image_data.animation_data;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.function.Predicate;

import spirite.base.brains.RenderEngine.TransformedHandle;
import spirite.base.graphics.GraphicsContext;
import spirite.base.graphics.GraphicsContext.Composite;
import spirite.base.image_data.Animation;
import spirite.base.image_data.GroupTree;
import spirite.base.image_data.GroupTree.GroupNode;
import spirite.base.image_data.GroupTree.LayerNode;
import spirite.base.image_data.GroupTree.Node;
import spirite.base.image_data.ImageWorkspace;
import spirite.base.image_data.ImageWorkspace.AdditionChange;
import spirite.base.image_data.ImageWorkspace.DeletionChange;
import spirite.base.image_data.ImageWorkspace.MoveChange;
import spirite.base.image_data.ImageWorkspace.StructureChange;
import spirite.base.image_data.ImageWorkspace.StructureChangeEvent;
import spirite.base.image_data.UndoEngine.NullAction;
import spirite.base.image_data.animation_data.FixedFrameAnimation.AnimationLayerBuilder.BuildFrame;
import spirite.base.util.MUtil;

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
	private final ImageWorkspace context;

	public FixedFrameAnimation(GroupNode group) {
		context = group.getContext();
		layers.add( constructFromGroup(group));
		name = group.getName();
		recalculateMetrics();
	}
	
	public FixedFrameAnimation( String name, ImageWorkspace workspace) {
		this.context = workspace;
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
	public void drawFrame(GraphicsContext gc, float t) {
		int _t = (int)Math.floor(t);
		List<TransformedHandle> drawList = getDrawListForFrame(_t);
		
		float alpha = gc.getAlpha();
		Composite comp = gc.getComposite();
		for( TransformedHandle renderable : drawList) {
			gc.setComposite(renderable.comp, renderable.alpha);
			renderable.handle.drawLayer( gc, renderable.trans);
		}
		gc.setComposite(comp, alpha);
		
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
			
			LayerNode node = layer.getLayerForMet(localMet);
			
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
							layer.nodeRemoved( (LayerNode) addition.node);
						else
							layer.nodeAdded( (LayerNode) addition.node);
					}
				}
				else if( change instanceof DeletionChange) {
					DeletionChange deletion = (DeletionChange)change;
					
					if( deletion.parent == layer.group
							&& layer.getLayers().contains(deletion.node)) 
					{
						if( evt.reversed)
							layer.nodeAdded( (LayerNode) deletion.node);
						else
							layer.nodeRemoved( (LayerNode)deletion.node);
					}
				}
				else if( change instanceof MoveChange) {
					MoveChange movement = (MoveChange)change;
					
					if( movement.moveNode instanceof LayerNode) {
						if( movement.oldParent == layer.group &&
							movement.oldParent == movement.newParent) {
							layer.nodeMoved( (LayerNode) movement.moveNode);
						}
						else if( movement.oldParent == layer.group) {
							if( evt.reversed)
								layer.nodeAdded( (LayerNode) movement.moveNode);
							else
								layer.nodeRemoved((LayerNode) movement.moveNode);
						}
						else if( movement.newParent == layer.group) {
							if( evt.reversed)
								layer.nodeRemoved( (LayerNode) movement.moveNode);
							else
								layer.nodeAdded((LayerNode) movement.moveNode);
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
		EMPTY,
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
			public AnimationLayer getLayerContext( ) {return AnimationLayer.this;}
			
			public void setLength( int newLength) {
				if( newLength < 0) 
					throw new IndexOutOfBoundsException();
				if( length == newLength)
					return;
				
				// Kind of ugly, but since Frames are generally only produced
				//	indirectly by links to other things, you can't rely on
				//	the frame objects themselves being stored and saved in any
				//	UndoAction, so the only reliable thing is the frame position
				int oldLength = length;
				int frame = getLayerContext().frames.indexOf(this);
				context.getUndoEngine().performAndStore( new NullAction() {
					@Override protected void undoAction() {
						getLayerContext().frames.get(frame).length = oldLength;
						_triggerChange();
					}
					
					@Override protected void performAction() {
						getLayerContext().frames.get(frame).length = newLength;
						_triggerChange();
					}
					@Override public String getDescription() {
						return "Change Frame Length";
					}
				});
			}
			
		}

		protected GroupNode group;
		protected String name;
		protected final ArrayList<Frame> frames = new ArrayList<>();
		protected boolean asynchronous = false;
		
		private AnimationLayer() {}

		public String getName() {
			return (group == null)?name:group.getName();
		}
		public void setName(String name) {
			this.name = name;
		}
		
		// :::: Direct Action
		
		/*** Adds an empty frame to fill the gap starting at StartTick of length length. */
		public void addGap( int startTick, int length) {
			Frame frame = getFrameForMet(startTick);
			int before = frames.indexOf(getFrameForMet(startTick));
			int start = (before == -1) ? startTick : Math.min(startTick,frames.get(before).getStart());
			
			Frame toAdd = new Frame(null, start-startTick + length, Marker.EMPTY);
			context.getUndoEngine().performAndStore( new NullAction() {
				@Override protected void undoAction() {
					frames.remove(toAdd);
					_triggerChange();
				}
				
				@Override protected void performAction() {
					if( before == -1)
						frames.add(toAdd);
					else
						frames.add(before,toAdd);
					_triggerChange();
				}
				@Override
				public String getDescription() {
					return "Add Animation Gap";
				}
			});
		}
		
		/** Moves a given frame into this animation layer at the given startTick.
		 * 
		 *  Note: does not necessarily have to be a frame from this AnimationLayer
		 *  or even this animation. */
		public void moveFrame( Frame frameToMove, int startTick, boolean above) {
			Frame frame = getFrameForMet( startTick);
			int before = frames.indexOf(frame);
			int length = frameToMove.length;
			
			if( frame == null)
				context.moveInto( frameToMove.getLayerNode(), group, true);
			else if( above)
				context.moveAbove(frameToMove.getLayerNode(), frame.getLayerNode());
			else 
				context.moveBelow(frameToMove.getLayerNode(), frame.getLayerNode());
			//context.getUndoEngine().pause();

			// TODO: Not entirely properly undoable
			// TODO: Make it erase gaps
		}
		
		public void addNode( LayerNode toAdd, Frame frameBefore) {
			
		}
		public void removeNode( LayerNode toAdd, Frame frameBefore) {
			
		}
		
		// :::: Link Interpretation
		public void nodeMoved(LayerNode moveNode) {
			Iterator<Frame> it = frames.iterator();
			while( it.hasNext()) {
				Frame frame = it.next();
				
				if( frame.getLayerNode() == moveNode) {
					it.remove();
					frameAdded(moveNode,frame);
					return;
				}
			}
		}
		public void nodeRemoved(LayerNode node) {
			frames.removeIf( new Predicate<Frame>() {
				@Override public boolean test(Frame t) {
					return ( t.node == node);
				}
			});
		}
		public void nodeAdded(LayerNode node) { 
			frameAdded( node, new Frame(node, 1, Marker.FRAME));
		}
		private void frameAdded(LayerNode node, Frame frame) { 
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
		
		public Frame getFrameForMet( int met) {
			int wm = 0;
			for( int i=0; i < frames.size(); ++i) {
				Frame frame = frames.get(i);
				if( met - wm < frame.length)
					return frame;
				wm += frame.length;
			}
			
			return null;
		}
		
		public LayerNode getLayerForMet( int met) {
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
				case EMPTY:
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
