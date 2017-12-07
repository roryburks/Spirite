package spirite.base.image_data.mediums.maglev;

import spirite.base.graphics.GraphicsContext;
import spirite.base.graphics.IImage;
import spirite.base.graphics.RawImage;
import spirite.base.image_data.selection.ALiftedData;
import spirite.base.image_data.selection.SelectionMask;
import spirite.hybrid.HybridHelper;

public class MaglevLiftedData extends ALiftedData {
	public final MaglevMedium medium;
	public final int iox, ioy;
	
	public MaglevLiftedData(MaglevMedium medium, SelectionMask selection) {
		this.medium = medium;
		this.iox = selection.getOX();
		this.ioy = selection.getOY();
		medium.Build();
	}

	@Override
	public void drawLiftedData(GraphicsContext gc) {
		gc.drawImage(medium.getBuiltImage().getBase(), medium.getBuiltImage().getXOffset()-iox, medium.getBuiltImage().getYOffset()-ioy);
	}

	@Override public int getWidth() {return medium.getWidth();}
	@Override public int getHeight() {return medium.getHeight();}

	@Override
	public IImage readonlyAccess() {
		// Somewhat wasteful that we turn the existing built image into a new image
		//	with a little more padding, but simpler an interface perspective
		RawImage img = HybridHelper.createImage(medium.getWidth() + medium.getBuiltImage().getXOffset()-iox,
				medium.getHeight() + medium.getBuiltImage().getYOffset()-ioy);
		
		img.getGraphics().drawImage(medium.getBuiltImage().getBase(), medium.getBuiltImage().getXOffset()-iox, medium.getBuiltImage().getYOffset()-ioy);
		return img;
	}

}
