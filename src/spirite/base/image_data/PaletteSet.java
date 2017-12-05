package spirite.base.image_data;

import spirite.base.brains.PaletteManager;
import spirite.base.brains.PaletteManager.Palette;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class PaletteSet {
	public final ImageWorkspace context;
	private final PaletteManager paletteManager;
	
	PaletteSet( ImageWorkspace workspace) {
		this.context = workspace;
		this.paletteManager = workspace.getPaletteManager();

		palettes.add(paletteManager.new Palette("Default"));
		selectedPalette = 0;
	}

	private List<Palette> palettes = new ArrayList<>();
	private int selectedPalette;
	
	public Palette getCurrentPalette() {
		return palettes.get(selectedPalette);
	}
	public void resetPalettes(Collection<Palette> newPalettes) {
		palettes = new ArrayList<>(newPalettes.size());
		palettes.addAll(newPalettes);
		selectedPalette = 0;
	}
	public void addPalette( Palette palette, boolean select) {
		palettes.add(palette);
		if( select)
			selectedPalette = palettes.size()-1;
		paletteManager.triggerPaletteChange();
	}
	public void removePalette( int i) {
		if( i < 0 || i >= palettes.size())
			return;
		
		palettes.remove(i);
		if( selectedPalette >= i)
			selectedPalette--;
		if( palettes.size() == 0) {
			palettes.add(paletteManager.new Palette("Default"));
			selectedPalette = 0;
		}
		paletteManager.triggerPaletteChange();
	}
	public void setSelectedPalette(int i) {
		selectedPalette = i;
		paletteManager.triggerPaletteChange();
	}
	public List<Palette> getPalettes() { return new ArrayList<>(palettes);}
}
