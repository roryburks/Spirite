package spirite.base.image_data.animation_data;

import java.awt.Frame;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.function.Predicate;

import spirite.base.graphics.GraphicsContext;
import spirite.base.graphics.GraphicsContext.Composite;
import spirite.base.graphics.RenderProperties;
import spirite.base.graphics.renderer.RenderEngine.TransformedHandle;
import spirite.base.image_data.Animation;
import spirite.base.image_data.AnimationManager.AnimationState;
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
import spirite.hybrid.MDebug;
import spirite.hybrid.MDebug.ErrorType;
import spirite.hybrid.MDebug.WarningType;

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

	public FixedFrameAnimation(GroupNode group, String name) {
		super( group.getContext());
		layers.add( constructFromGroup(group));
		this.name = name;
		recalculateMetrics();
	}
	
	public FixedFrameAnimation( String name, ImageWorkspace workspace) {
		super( workspace);
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
	@Override public boolean isFixedFrame() {return true;}
	
	
	private AnimationLayer constructFromGroup( GroupNode group) {

		AnimationLayer layer = new AnimationLayer();
		layer.group = group;
		name = group.getName();
		
		ListIterator<GroupTree.Node> it = group.getChildren().listIterator(group.getChildren().size());
		
		while( it.hasPrevious()) {
			GroupTree.Node node = it.previous();
			
			if( node instanceof LayerNode)
				layer.frames.add( layer.new Frame((LayerNode) node, 1, Marker.FRAME));
		}
		
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
		List<TransformedHandle> drawList = getDrawList(_t);
		
		float alpha = gc.getAlpha();
		Composite comp = gc.getComposite();
		for( TransformedHandle renderable : drawList) {
			gc.setComposite(comp, renderable.alpha);
			renderable.handle.drawLayer( gc, renderable.trans);
		}
		gc.setComposite(comp, alpha);
	}
	@Override
	public List<TransformedHandle> getDrawList(float t) {
		int _t = (int)Math.floor(t);
		
		int met = MUtil.cycle(startFrame, endFrame, _t);
		
		List<TransformedHandle> drawList = new ArrayList<>();
		
		for( AnimationLayer layer : layers) {
			if( layer.getFrames().size() == 0) continue;
			
			int start = layer.getStart();
			int end = layer.getEnd();
			int localMet = met;


			// Based on the layer timing type, determine the local frame
			//	index to use (if any)
			if( layer.asynchronous) {
				localMet = MUtil.cycle(start, end, _t);
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
	public List<List<TransformedHandle>> getDrawTable( float t, AnimationState state) {
		int T = (int)Math.floor(t);
		int L = endFrame-startFrame;

		List<List<TransformedHandle>> drawTable = new ArrayList<>();
		for( int i= -(L-1)/2; i< (L)/2; ++i) {
			List<TransformedHandle> drawList = new ArrayList<>();
			int met = MUtil.cycle(startFrame, endFrame-1, i + T);
			
			if( !state.getSubstateForRelativeTick( i).isVisible())
				continue;
			
			// START (mostly) DUPLICATE CODE FROM getDrawList
			for( AnimationLayer layer : layers) {
				if( layer.getFrames().size() == 0) continue;
				
				RenderProperties properties = state.getPropertiesForFrame(layer, i);
				if( !properties.isVisible()) continue;
				
				int start = layer.getStart();
				int end = layer.getEnd();
				int localMet = met;

				// Based on the layer timing type, determine the local frame
				//	index to use (if any)
				if( layer.asynchronous) {
					localMet = MUtil.cycle(start, end, i + T);
				}
				
				LayerNode node = layer.getLayerForMet(localMet);
				
				if( node != null) {
					for( TransformedHandle tr  : node.getLayer().getDrawList()) {
						tr.trans.translate(node.getOffsetX(), node.getOffsetY());
						tr.alpha = properties.alpha;
						tr.method = properties.method;
						tr.renderValue = properties.renderValue;
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
			// END (mostly) DUPLICATE CODE FROM getDrawList
			
			if( !drawList.isEmpty())
				drawTable.add(drawList);
		}
		
		
		
		
		return drawTable;
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
				// Ugly?
				List<Integer> carets = new ArrayList<>(1);
				carets.add(0, 0);
				int loopDepth = 0;
				for( int index = 0; index < frames.size(); ++index) {
					if( frames.get(index) == this)
						return carets.get(loopDepth);
					carets.set(loopDepth, carets.get(loopDepth) + frames.get(index).length);
					if( frames.get(index).marker == Marker.START_LOCAL_LOOP) {
						carets.add(loopDepth + 1, carets.get(loopDepth) - frames.get(index).length);
						loopDepth++;
					}
					if( frames.get(index).marker == Marker.END_LOCAL_LOOP) {
						carets.remove(loopDepth);
						loopDepth--;
					}
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
			
			public int getLoopDepth() {
				int depth = 0;
				for( Frame f : frames) {
					if( f == this)
						return depth;
					if( f.marker == Marker.START_LOCAL_LOOP)
						depth += 1;
					if( f.marker == Marker.END_LOCAL_LOOP)
						depth -= 1;
				}
				if( depth != 0)
					MDebug.handleError(ErrorType.STRUCTURAL, "Start-End loop mismatch");
				return depth;
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
		
		// =============================
		// ==== State Based Actions ====
		private void performFrameStateChange( final List<Frame> newStructure, String description) {
			final List<Frame> oldList = new ArrayList<>(frames);
			context.getUndoEngine().performAndStore( new NullAction() {
				@Override
				protected void performAction() {
					frames.clear();
					frames.addAll(newStructure);
					_triggerChange();
				}

				@Override
				protected void undoAction() {
					frames.clear();
					frames.addAll(oldList);
					_triggerChange();
				}
				@Override
				public String getDescription() {
					return description;
				}
			});
		}
		
		/*** Adds an empty frame to fill the gap starting at StartTick of length length. */
		public void addGap( int startTick, int length) {
			int before = frames.indexOf(getFrameForMet(startTick));
			int start = (before == -1) ? startTick : Math.min(startTick,frames.get(before).getStart());
			
			Frame toAdd = new Frame(null, start-startTick + length, Marker.EMPTY);
			List<Frame> newStructure = new ArrayList<>(frames.size()+1);
			newStructure.addAll( frames.subList(0, before));
			newStructure.add(toAdd);
			newStructure.addAll( frames.subList(before, frames.size()));
			performFrameStateChange(newStructure, "Add Animation Gap");
		}
		
		/** Adds a START_LOCAL_LOOP before this frame and a END_LOCAL_LOOP after it. */
		public void wrapInLoop(Frame frame) {
			int before = frames.indexOf(frame);

			Frame SoL = new Frame(null, frame.length, Marker.START_LOCAL_LOOP);
			Frame EoL = new Frame(null, 0, Marker.END_LOCAL_LOOP);
			List<Frame> newStructure = new ArrayList<>(frames.size()+2);
			newStructure.addAll(frames.subList(0, before));
			newStructure.add(SoL);
			newStructure.add(frame);
			newStructure.add(EoL);
			newStructure.addAll(frames.subList(before+1, frames.size()));
			performFrameStateChange(newStructure, "Added Local Loop");
			
		}
		
		/** Re-arranges a START_LOCAL_LOOP frame such that it re-wraps around the defined bounds. 
		 * @param doNotCut if true, it will only resize up until the closest SoF if nested 
		 * */
		public void reWrap(Frame sofFrame, int start, int end, boolean doNotCut) {
			if( sofFrame.getMarker() != Marker.START_LOCAL_LOOP) {
				MDebug.handleWarning(WarningType.STRUCTURAL, this, "Tried to re-wrap something other than a Start of Local Loop");
				return;
			}
			int sofIndex = frames.indexOf(sofFrame);
			int eofIndex = sofIndex;
			while( frames.get(++eofIndex).marker != Marker.END_LOCAL_LOOP);
			Frame eofFrame  = frames.get(eofIndex);
			

			List<Frame> newStructure = new ArrayList<>(frames.size());
			newStructure.addAll(frames.subList(0, start));
			newStructure.add(sofFrame);
			newStructure.addAll(frames.subList(start+1, end));
			newStructure.add(eofFrame);
			newStructure.addAll(frames.subList(end+1, frames.size()));
			performFrameStateChange(newStructure, "Resized Local Loop");
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
		
		// ============================
		// ==== Link Interpretation ====
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
				
			frames.add( Math.max(frames.size()-1, 0), frame);
		}


		// =============================
		// ==== Frame Index Methods ====
		public int getStart() {
			return 0;
		}
		public int getEnd() {
			int caret = 0;
			for( int i=0; i<frames.size(); ++i) {
				caret += frames.get(i).length;
				if( frames.get(i).marker == Marker.START_LOCAL_LOOP)
					while( frames.get(++i).marker != Marker.END_LOCAL_LOOP);
			}
			
			return caret;
		}
		
		public Frame getFrameForMet( int met) {
			return getFrameForMet( met, false);
		}
		public Frame getFrameForMet( int met, boolean noLoop) {
			int caret = 0;
			int index = 0;
			int loopLen = 0;
			
			if( frames.isEmpty())
				return null;
			
			while( true) {
				Frame frame = frames.get(index++);	// Watch the early increment
				if( (met - caret) < frame.length ) {
					switch( frame.marker) {
					case START_LOCAL_LOOP:
						return _getFrameFromLocalLoop( index, met-caret);
					case FRAME:
						return frame;
					case END_LOCAL_LOOP:
						MDebug.handleWarning(WarningType.STRUCTURAL, this, "Malformed Animation (END_LOCAL_LOOP with length > 1)");
					case EMPTY:
						return null;
					}
				}
				if( frame.marker == Marker.START_LOCAL_LOOP)
					while( frames.get(index).marker != Marker.END_LOCAL_LOOP) index++;
				
				if( index == frames.size()) {
					if( noLoop || loopLen == 0) 
						return null;
					index = 0;
				}
				
				loopLen += frame.length;
				caret += frame.length;
			}
		}
		
		private Frame _getFrameFromLocalLoop( int start, int offset) {
			int index = start;
			int caret = offset;
			int loopLen = 0;
			
			while( true) {
				Frame frame = frames.get(index);
				
				if( (offset - caret) < frame.length) {
					switch( frame.marker) {
					case START_LOCAL_LOOP:
						return _getFrameFromLocalLoop( index, offset-caret);
					case FRAME:
						return frame;
					case END_LOCAL_LOOP:
						if( loopLen == 0)
							return null;
						else 
							index = 0;
						break;
					case EMPTY:
						return null;
					}
				}
				loopLen += frame.length;
				caret += frame.length;
			}
		}
		
		public LayerNode getLayerForMet( int met) {
			Frame f = getFrameForMet(met);
			return (f == null) ? null : f.node;
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
	@Override
	public void purge() {
		Iterator<AnimationLayer> it = layers.iterator();
		while(it.hasNext() ) {
			AnimationLayer layer = it.next();
			if( layer.group != null && !context.nodeInWorkspace(layer.group))
				it.remove();
		}		
	}
}
