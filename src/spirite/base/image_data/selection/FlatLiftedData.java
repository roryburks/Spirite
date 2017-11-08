package spirite.base.image_data.selection;

import spirite.base.graphics.GraphicsContext;
import spirite.base.graphics.IImage;
import spirite.base.graphics.RawImage;

/**
 * A simple LiftedData containing an IImage
 * 
 * Immutable
 */
public class FlatLiftedData extends ALiftedData {
	private final IImage lifted;
	
	public FlatLiftedData( IImage lifted) {
		this.lifted = lifted;
	}

	@Override
	public ALiftedData asType( Class<? extends ALiftedData> tclass) {
		if( tclass == FlatLiftedData.class)
			return this;
		return null;
	}


	@Override
	public void drawLiftedData(GraphicsContext gc) {
		gc.drawImage(lifted, 0, 0);
	}

	@Override
	public int getWidth() {
		return lifted.getWidth();
	}

	@Override
	public int getHeight() {
		return lifted.getHeight();
	}

	@Override
	public IImage readonlyAccess() {
		return lifted;
	}
}
