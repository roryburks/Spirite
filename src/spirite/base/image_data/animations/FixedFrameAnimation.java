package spirite.base.image_data.animations;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import spirite.base.graphics.GraphicsContext;
import spirite.base.graphics.GraphicsContext.Composite;
import spirite.base.graphics.RenderProperties;
import spirite.base.graphics.renderer.RenderEngine.TransformedHandle;
import spirite.base.image_data.Animation;
import spirite.base.image_data.AnimationManager.AnimationState;
import spirite.base.image_data.GroupTree.GroupNode;
import spirite.base.image_data.GroupTree.LayerNode;
import spirite.base.image_data.GroupTree.Node;
import spirite.base.image_data.UndoEngine.NullAction;
import spirite.base.image_data.ImageWorkspace;
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

	public FixedFrameAnimation(GroupNode group, String name, boolean includeSubtrees) {
		super( group.getContext());
		layers.add( constructFromGroup(group, includeSubtrees));
		this.name = name;
		recalculateMetrics();
	}
	public FixedFrameAnimation( String name, ImageWorkspace workspace) {
		super( workspace);
		this.name = name;
	}
	
	public void addBuiltLinkedLayer( GroupNode link, Map<Node,FrameAbstract> frameMap, boolean includeSubtrees) {
		
		AnimationLayer layer = new AnimationLayer();
		layer.groupLink = link;		
		layer.nodeLinks = new HashMap<>();
		
		for( Entry<Node,FrameAbstract> entry : frameMap.entrySet()) {
			layer.nodeLinks.put(entry.getKey(), layer.new Frame(entry.getValue()));
		}
		
		layer.includeSubtrees = includeSubtrees;
		layer.groupLinkUpdated();
		layers.add(layer);
		_triggerChange();
	}
	
	@Override public boolean isFixedFrame() {return true;}
	
	
	private AnimationLayer constructFromGroup( GroupNode group, boolean includeSubtrees) {

		AnimationLayer layer = new AnimationLayer();
		layer.groupLink = group;
		layer.includeSubtrees = includeSubtrees;
		layer.groupLinkUpdated();
		
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
	
	@Override public float getStartFrame() { return startFrame; }
	@Override public float getEndFrame() { return endFrame-1; }
	public int getStart() { return startFrame; }
	public int getEnd() { return endFrame-1; }
	
	@SuppressWarnings("unchecked")
	public List<AnimationLayer> getLayers() {
		return (List<AnimationLayer>) layers.clone();
	}

	// =================
	// ==== Drawing ====
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
	public void nodesChanged( List<Node> nodes) {
		for( AnimationLayer layer : layers) {
			layer.groupLinkUpdated();
		}

		_triggerChange();
	}
	
	@Override
	public List<Node> getNodeLinks() {
		List<Node> list = new ArrayList<>(layers.size());
		
		for( AnimationLayer layer : layers) 
			if( layer.groupLink != null) 
				list.add(layer.groupLink);
		
		return list;
	}
	
	
	public void importGroup(GroupNode node, boolean includeSubtrees) {
		layers.add( constructFromGroup(node, includeSubtrees));
		_triggerChange();
	}
	
	
	
	public enum Marker {
		FRAME,
		START_LOCAL_LOOP,	
		END_LOCAL_LOOP,
	}
	public class AnimationLayer {
		protected GroupNode groupLink;
		protected String name;
		protected final ArrayList<Frame> frames = new ArrayList<>();
		protected boolean asynchronous = false;

		// Irrelevant if groupLink == null
		protected Map<Node,Frame> nodeLinks;
		protected boolean includeSubtrees = false;
		
		private AnimationLayer() {}

		public String getName() { return (groupLink==null)?name:groupLink.getName();}
		public void setName(String name) {this.name = name;}
		public boolean isAsynchronous() {return asynchronous;}
		public void setAsynchronous(boolean asynchronous) {this.asynchronous = asynchronous;}
		public boolean includesSubtrees() {return includeSubtrees;}
		
		public void setIncludeSubTrees( final boolean newInclude) {
			final boolean oldInclude = includeSubtrees;
			context.getUndoEngine().performAndStore( new NullAction() {
				protected void performAction() {
					includeSubtrees = newInclude;
					_triggerChange();
				}
				protected void undoAction() {
					includeSubtrees = oldInclude;
					_triggerChange();
				}
				
			});
		}

		public GroupNode getGroupLink() {return groupLink;}
		public List<Frame> getFrames() { return new ArrayList<>(frames); }
		
		// =========================================================
		// ==== State Based Actions (for non-linked Animations) ====
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
//		public void reWrap(Frame sofFrame, int start, int end, boolean doNotCut) {
//			if( sofFrame.getMarker() != Marker.START_LOCAL_LOOP) {
//				MDebug.handleWarning(WarningType.STRUCTURAL, this, "Tried to re-wrap something other than a Start of Local Loop");
//				return;
//			}
//			int sofIndex = frames.indexOf(sofFrame);
//			int eofIndex = sofIndex;
//			while( frames.get(++eofIndex).marker != Marker.END_LOCAL_LOOP);
//			Frame eofFrame  = frames.get(eofIndex);
//			
//
//			List<Frame> newStructure = new ArrayList<>(frames.size());
//			newStructure.addAll(frames.subList(0, start));
//			newStructure.add(sofFrame);
//			newStructure.addAll(frames.subList(start+1, end));
//			newStructure.add(eofFrame);
//			newStructure.addAll(frames.subList(end+1, frames.size()));
//			performFrameStateChange(newStructure, "Resized Local Loop");
//		}
		
		/** Moves a given frame into this animation layer at the given startTick.
		 * 
		 *  Note: does not necessarily have to be a frame from this AnimationLayer
		 *  or even this animation. */
		public void moveFrame( Frame frameToMove, Frame frameRelativeTo, boolean above) {
			
			if( groupLink == null) {
				// TODO
			}
			else {
				if( frameRelativeTo == null)
					context.moveInto( frameToMove.getLinkedNode(), groupLink, true);
				else if( above)
					context.moveAbove(frameToMove.getLinkedNode(), frameRelativeTo.getLinkedNode());
				else 
					context.moveBelow(frameToMove.getLinkedNode(), frameRelativeTo.getLinkedNode());				
			}
		}
		
		// ============================
		// ==== Link Interpretation ====
		private void groupLinkUpdated() {
			if( groupLink != null) {
				Map<Node,Frame> oldMap = nodeLinks;
				Map<Node,Frame> newMap = new HashMap<>();
				frames.clear();
				
				_gluRec( oldMap, newMap, groupLink);
				
				nodeLinks = newMap;
			}
		}
		private int _gluRec( Map<Node,Frame> oldMap, Map<Node,Frame> newMap, GroupNode node) {
			int len = 0;
			List<Node> children = node.getChildren();
			for( int i=children.size()-1; i>=0; --i) {
				Node child = children.get(i);
				if( child instanceof GroupNode && includeSubtrees) {
					boolean usingNew = (oldMap == null || !oldMap.containsKey(child));
					Frame solFrame = (usingNew) 
							? new Frame(child, 0, Marker.START_LOCAL_LOOP)
							: oldMap.get(child);
					
					frames.add(solFrame);
					int subLen = _gluRec( oldMap, newMap, (GroupNode)child);
					if( usingNew || solFrame.length < subLen)	// Could be a better way to do this
						solFrame.length = subLen;
					frames.add(new Frame(null, 0, Marker.END_LOCAL_LOOP));
					
					len += solFrame.length;
					newMap.put(child, solFrame);
				}
				else if( child instanceof LayerNode) {
					Frame newFrame = (oldMap == null || !oldMap.containsKey(child))
							? new Frame( (LayerNode) child, 1, Marker.FRAME)
							: oldMap.get(child);
					frames.add(newFrame);
					
					len += newFrame.length;
					newMap.put(child, newFrame);
				}
			}
			return len;
		}


		// =============================
		// ==== Frame Index Methods ====
		public int getStart() { return 0; }
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
				loopLen += frame.length;
				if( (met - caret) < frame.length ) {
					switch( frame.marker) {
					case START_LOCAL_LOOP:
						return _getFrameFromLocalLoop( index, met-caret);
					case FRAME:
						return (frame.isInGap(met-caret) ? null : frame);
					case END_LOCAL_LOOP:
						MDebug.handleWarning(WarningType.STRUCTURAL, this, "Malformed Animation (END_LOCAL_LOOP with length > 1)");
					}
				}
				if( frame.marker == Marker.START_LOCAL_LOOP)
					while( frames.get(index).marker != Marker.END_LOCAL_LOOP) index++;
				
				if( index == frames.size()) {
					if( noLoop || loopLen == 0) 
						return null;
					index = 0;
				}

				caret += frame.length;
			}
		}
		
		private Frame _getFrameFromLocalLoop( int start, int offset) {
			int index = start;
			int caret = 0;
			int loopLen = 0;
			
			while( true) {
				Frame frame = frames.get(index++);
				
				if( (offset - caret) < frame.length) {
					switch( frame.marker) {
					case START_LOCAL_LOOP:
						return _getFrameFromLocalLoop( index, offset-caret);
					case FRAME:
						return frame.isInGap(offset-caret) ? null : frame;
					case END_LOCAL_LOOP:
						return null;
					}
				}
				if( frame.marker == Marker.END_LOCAL_LOOP) {
					if( loopLen == 0)
						return null;
					index = start;
				}
				loopLen += frame.length;
				caret += frame.length;
			}
		}
		
		public LayerNode getLayerForMet( int met) {
			Frame f = getFrameForMet(met);
			return (f == null) ? null : (LayerNode)f.node;
		}
		

		public class Frame extends FrameAbstract {
			private Frame( Node child, int length, Marker marker) {
				super(child, length, marker, 0, 0);
			}
			private Frame( LayerNode node, int length, Marker marker, int gapBefore, int gapAfter) {
				super(node, length, marker, gapBefore, gapAfter);
			}
			private Frame( FrameAbstract other) {
				super( other);
			}
			public boolean isInGap(int internalMet) {
				return ( internalMet < gapBefore || (length-1) - internalMet < gapAfter);
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
			public Node getLinkedNode() { return node; }
			public Marker getMarker() { return marker;}
			public AnimationLayer getLayerContext( ) {return AnimationLayer.this;}
			public int getGapBefore() {return gapBefore;}
			public int getGapAfter() {return gapAfter;}
			
			public void setLength( final int newLength) {
				if( newLength < 0) 
					throw new IndexOutOfBoundsException();
				if( length == newLength)
					return;
				
				final int oldLength = length;
				context.getUndoEngine().performAndStore( new NullAction() {
					protected void performAction() { 
						length = newLength;
						_triggerChange();
					}
					protected void undoAction() { 
						length = oldLength;
						_triggerChange();
					}
					public String getDescription() { return "Resize Animation Frame Length";}
				});
			}
			public void setGapBefore( final int newGap) {
				if( newGap < 0) 
					throw new IndexOutOfBoundsException();
				if( gapBefore == newGap)
					return;

				final int oldGap = gapBefore;
				final int oldLength = length;
				final int newLength = length + (newGap-oldGap);
				context.getUndoEngine().performAndStore( new NullAction() {
					protected void performAction() {
						gapBefore = newGap; 
						length = newLength;
						_triggerChange();
					}
					protected void undoAction() { 
						gapBefore = oldGap;
						length = oldLength;
						_triggerChange();
					}
					public String getDescription() { return "Resize Animation Frame Gaps";}
				});
			}
			public void setGapAfter( final int newGap) {
				if( newGap < 0) 
					throw new IndexOutOfBoundsException();
				if( gapAfter == newGap)
					return;
				
				final int oldGap = gapAfter;
				final int oldLength = length;
				final int newLength = length + (newGap-oldGap);
				context.getUndoEngine().performAndStore( new NullAction() {
					protected void performAction() { 
						gapAfter = newGap; 
						length = newLength;
						_triggerChange();
					}
					protected void undoAction() { 
						gapAfter = oldGap;
						length = oldLength;
						_triggerChange();
					}
					public String getDescription() { return "Resize Animation Frame Gaps";}
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

			public Frame next() {
				int i = this.getLayerContext().frames.indexOf(this);
				if( i == -1 || (i+1) >= this.getLayerContext().frames.size())
					return null;
				else
					return this.getLayerContext().frames.get(i+1);
			}
		}
	}

	public static class FrameAbstract {
		protected int length;
		protected Node node;
		protected Marker marker;
		protected int gapBefore;
		protected int gapAfter;

		public FrameAbstract() {}
		public FrameAbstract(FrameAbstract other) {
			this.length = other.length;
			this.node = other.node;
			this.marker = other.marker;
			this.gapBefore = other.gapBefore;
			this.gapAfter = other.gapAfter;
		}
		public FrameAbstract( Node node, int length, Marker marker, int gapBefore, int gapAfter) {
			this.length = length;
			this.node = node;
			this.marker = marker;
			this.gapAfter = gapAfter;
			this.gapBefore = gapBefore;
		}
	}
	
	@Override
	public void purge() {
		Iterator<AnimationLayer> it = layers.iterator();
		while(it.hasNext() ) {
			AnimationLayer layer = it.next();
			if( layer.groupLink != null && !context.nodeInWorkspace(layer.groupLink))
				it.remove();
		}		
	}
}
