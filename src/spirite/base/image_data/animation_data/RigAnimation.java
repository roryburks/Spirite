package spirite.base.image_data.animation_data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.SortedMap;
import java.util.TreeMap;

import spirite.base.graphics.GraphicsContext;
import spirite.base.graphics.renderer.RenderEngine.RenderMethod;
import spirite.base.graphics.renderer.RenderEngine.TransformedHandle;
import spirite.base.image_data.Animation;
import spirite.base.image_data.AnimationManager.AnimationState;
import spirite.base.image_data.GroupTree.GroupNode;
import spirite.base.image_data.ImageWorkspace.StructureChangeEvent;
import spirite.base.image_data.animation_data.RigAnimation.RigAnimLayer.PartFrames;
import spirite.base.image_data.layers.SpriteLayer;
import spirite.base.image_data.layers.SpriteLayer.Part;
import spirite.base.image_data.layers.SpriteLayer.PartStructure;
import spirite.base.util.glmath.MatTrans;
import spirite.base.util.interpolation.CubicSplineInterpolatorND;

public class RigAnimation extends Animation {
	public static class PartKeyFrame {
		float tx;
		float ty;
		float sx = 1f;
		float sy = 1f;
		float rot;
		PartKeyFrame() {}
		PartKeyFrame(float tx, float ty, float sx, float sy, float rot) {
			this.tx = tx;
			this.ty = ty;
			this.sx = sx;
			this.sy = sy;
			this.rot = rot;
		}
	}
	
	public class RigAnimLayer {
		private final Map<Part, PartFrames> map = new HashMap<>();
		public final SpriteLayer sprite;
		
		RigAnimLayer( SpriteLayer sprite) {
			this.sprite = sprite;
		}
		public PartFrames getPartFrames( Part part) {
			PartFrames frames = map.get(part);
			if( frames == null) {
				frames = new PartFrames();
				map.put(part, frames);
			}
			return frames;
		}
		
		class PartFrames {
			TreeMap<Float, PartKeyFrame> frameMap = new TreeMap<>();
			
			// 0 : tx
			// 1 : ty
			// 2 : sx
			// 3 : sy
			// 4 : rot
			CubicSplineInterpolatorND interpolator = null;
			boolean interpolatorIsBuilt = false;

			
			public void addKeyFrame( float t, PartKeyFrame pkf) {
				frameMap.put(t, pkf);
				interpolatorIsBuilt = false;
			}
			public void removeKeyFrame( float t) {
				frameMap.remove((Float)t);
				interpolatorIsBuilt = false;
			}
			public void removeKeyFramesInRange( Float start, Float end) {
				Iterator<Entry<Float,PartKeyFrame>> it = frameMap.entrySet().iterator();
				while( it.hasNext()) {
					Entry<Float,PartKeyFrame> entry = it.next();
					if( (start == null || entry.getKey() >= start) &&
						(end == null || entry.getKey() <= end))
						it.remove();
				}
				interpolatorIsBuilt = false;
			}
			public void clearKeyFrames() { 
				frameMap.clear();
				interpolatorIsBuilt = false;
			}
			
			
			public PartKeyFrame getFrameAtT( float t) {
				buildInterpolator();
				if( interpolator == null)
					return null;
				
				float[] datum = interpolator.eval(t);
				return new PartKeyFrame(datum[0], datum[1], datum[2], datum[3], datum[4]);
			}
			
			
			/** Builds the interpolator if it needs to be updated.  NOTE: May be null. */
			private void buildInterpolator() {
				if( !interpolatorIsBuilt) {
					if( frameMap.isEmpty())
						interpolator = null;
					else {
						List<Entry<Float,PartKeyFrame>> entries = new ArrayList<>(frameMap.entrySet());
						float[][] data = new float[entries.size()][];
						
						System.out.println("BUILD");
						
						
						for( int i=0; i < entries.size(); ++i) {
							data[i] = new float[6];
							
							PartKeyFrame key = entries.get(i).getValue();
							data[i][0] = key.tx;
							data[i][1] = key.ty;
							data[i][2] = key.sx;
							data[i][3] = key.sy;
							data[i][4] = key.rot;
							data[i][5] = entries.get(i).getKey();
						}
						
						interpolator = new CubicSplineInterpolatorND(data, 5, entries.size(), false);
					}

					interpolatorIsBuilt = true;
				}
			}
		}
	}
	
	private final List<RigAnimLayer> rigLayers = new ArrayList<>();
	
	boolean interpolatorIsConstructed = false;
	
	public RigAnimation( SpriteLayer sprite, String name) {
		this.rigLayers.add( new RigAnimLayer(sprite));
		this.name = name;
		
		Random r = new Random();
		for( Part part : sprite.getParts()) {
			//PartFrames pfs = rigLayers.get(0).getPartFrames(part);
			for( int i=0; i<=10; ++i) {
				PartKeyFrame key = new PartKeyFrame();
				key.tx = r.nextFloat() * 100;
				key.ty = r.nextFloat() * 100;
				key.rot = r.nextFloat() * (float)(Math.PI*2);
				rigLayers.get(0).getPartFrames(part).frameMap.put((float)i, key);
			}
		}
	}
	
	public List<SpriteLayer> getSprites() {
		List<SpriteLayer> layer = new ArrayList<>(rigLayers.size());
		for( RigAnimLayer rail : rigLayers)
			layer.add(rail.sprite);
		
		return layer;
	}
	
	@Override
	public void drawFrame(GraphicsContext gc, float t) {
		rigLayers.get(0).sprite.draw(gc);
	}

	@Override
	public List<TransformedHandle> getDrawList(float t) {
		return rigLayers.get(0).sprite.getDrawList();
	}

	@Override
	public List<List<TransformedHandle>> getDrawTable(float t, AnimationState state) {
		List<List<TransformedHandle>> table = new ArrayList<>(rigLayers.size());
		
		for( RigAnimLayer rail : rigLayers) {
			List<Part> parts = rail.sprite.getParts();
			List<TransformedHandle> layer = new ArrayList<TransformedHandle>(parts.size());
			
			for( Part part : parts) {
				PartStructure pstruct = part.getStructure();
				PartFrames pf = rail.map.get(part);
				
				if( pf != null) {
					PartKeyFrame key = pf.getFrameAtT(state.getMetronom());
					if( key != null) {
						pstruct.transX = key.tx;
						pstruct.transY = key.ty;
						pstruct.scaleX = key.sx;
						pstruct.scaleY = key.sy;
						pstruct.rot = key.rot;
					}
				}
				
				MatTrans trans = new MatTrans();
				trans.preRotate(pstruct.rot);
				trans.preScale(pstruct.scaleX, pstruct.scaleY);
				trans.preTranslate(pstruct.transX, pstruct.transY);
				
				TransformedHandle th = new TransformedHandle();
				th.alpha = pstruct.alpha;
				th.depth = pstruct.depth;
				th.handle = pstruct.handle;
				th.trans = trans;
				layer.add(th);
			}
			
			table.add(layer);
		}
		
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
