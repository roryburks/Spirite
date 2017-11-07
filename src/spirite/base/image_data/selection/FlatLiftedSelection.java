package spirite.base.image_data.selection;

import spirite.base.graphics.GraphicsContext;
import spirite.base.graphics.IImage;
import spirite.base.graphics.RawImage;

public class FlatLiftedSelection extends ALiftedSelection {
	RawImage lifted;
	
	public FlatLiftedSelection( RawImage lifted) {
		this.lifted = lifted;
	}

	@Override
	public ALiftedSelection asType( Class<? extends ALiftedSelection> tclass) {
		if( tclass == FlatLiftedSelection.class)
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
