package spirite.base.pen.selection_builders;

import spirite.base.graphics.GraphicsContext;
import spirite.base.graphics.RawImage;
import spirite.base.image_data.ImageWorkspace;
import spirite.base.image_data.SelectionEngine.BuiltSelection;
import spirite.hybrid.HybridHelper;

public class OvalSelectionBuilder extends ASelectionBuilder {
	private int startX;
	private int startY;
	private int currentX;
	private int currentY;
	
	public OvalSelectionBuilder( ImageWorkspace ws) {super(ws);}
	
	@Override
	public void start(int x, int y) {
		startX = currentX = x;
		startY = currentY = y;
	}

	@Override
	public void update(int x, int y) {
		currentX = x;
		currentY = y;
	}
	@Override
	public RawImage build() {
		RawImage img = HybridHelper.createImage( context.getWidth(), context.getHeight());
		GraphicsContext gc = img.getGraphics();
		gc.fillOval(
				Math.min(startX, currentX), Math.min(startY, currentY),
				Math.abs(startX-currentX), Math.abs(startY-currentY));
		
		return img;
	}
	@Override
	public void draw(GraphicsContext g) {
		g.drawOval(
				Math.min(startX, currentX), Math.min(startY, currentY),
				Math.abs(startX-currentX), Math.abs(startY-currentY));
	}
}
