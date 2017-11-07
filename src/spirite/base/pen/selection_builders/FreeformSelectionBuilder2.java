package spirite.base.pen.selection_builders;

import spirite.base.graphics.GraphicsContext;
import spirite.base.graphics.RawImage;
import spirite.base.image_data.ImageWorkspace;
import spirite.base.image_data.SelectionEngine.BuiltSelection;
import spirite.base.util.Colors;
import spirite.base.util.compaction.IntCompactor;
import spirite.base.util.glmath.Vec2i;
import spirite.hybrid.HybridHelper;

public class FreeformSelectionBuilder2 extends ASelectionBuilder {
	IntCompactor compactor_x = new IntCompactor();
	IntCompactor compactor_y = new IntCompactor();
	
	public FreeformSelectionBuilder2( ImageWorkspace ws) {super(ws);}
	
	@Override
	public void start(int x, int y) {
		compactor_x.add(x);
		compactor_y.add(y);
	}

	@Override
	public void update(int x, int y) {
		compactor_x.add(x);
		compactor_y.add(y);
	}
	@Override
	public RawImage build() {
		RawImage img = HybridHelper.createImage( context.getWidth(), context.getHeight());

		GraphicsContext gc = img.getGraphics();
		gc.setColor(Colors.WHITE);
		gc.fillPolygon( compactor_x.toArray(), compactor_y.toArray(), compactor_x.size());
		
		return img;
	}

	@Override
	public void draw(GraphicsContext g) {
		for( int i=0; i < compactor_x.getChunkCount(); ++i) {
			g.drawPolyLine(compactor_x.getChunk(i), 
						compactor_y.getChunk(i), 
						compactor_x.getChunkSize(i));
		}
	}
	public Vec2i getStart() {
		return new Vec2i( compactor_x.get(0), compactor_y.get(0));
	}
	public Vec2i getEnd() {
		int s = compactor_x.size();
		return new Vec2i( compactor_x.get(s-1), compactor_y.get(s-1));
	}

}
