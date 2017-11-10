package spirite.base.pen.behaviors;

import spirite.base.graphics.RawImage;
import spirite.base.graphics.renderer.RenderEngine.RenderSettings;
import spirite.base.pen.Penner;
import spirite.base.util.MUtil;

public class PickBehavior extends StateBehavior {
	/**
	 * 
	 */
	private final Penner penner;
	final boolean leftClick;
	
	public PickBehavior( Penner penner, boolean leftClick) {
		super(penner);
		this.penner = penner;
		this.leftClick = leftClick;
	}
	
	private void pickColor() {
		// Get the composed image
		RenderSettings settings = new RenderSettings(
				this.penner.renderEngine.getDefaultRenderTarget(this.penner.workspace));
		RawImage img = this.penner.renderEngine.renderImage(settings);
		
		if( !MUtil.coordInImage(this.penner.x, this.penner.y, img))
			return;
		this.penner.paletteManager.setActiveColor( (leftClick)?0:1, img.getRGB(this.penner.x, this.penner.y));
	}

	@Override
	public void start() {
		pickColor();
	}
	@Override
	public void onMove() {
		pickColor();
	}
	@Override public void onTock() {}
}