package spirite.image_data;

import java.util.ArrayList;
import java.util.List;

/***
 *  The SelectionEngine controls the selected image data, moving it from
 *  layer to layer, workspace to workspace, and program-to-clipboard
 *  
 *  A "Selection" is essentially an alpha mask and a corresponding 
 *  BufferedImage which is floating outside of the ImageWorkspace until
 *  it is either merged into existing data or elevated to its own layer.
 *  
 * @author Rory Burks
 *
 */
public class SelectionEngine {
	
	

	// :::: Observers
	public static interface MSelectionObserver {
		public void selectionChanged(SelectionEvent evt);
		public void buildingSelection( SelectionEvent evt);
	}
	public static class SelectionEvent {
	}
    List<MSelectionObserver> selectionObservers = new ArrayList<>();

    void triggerSelectionChanged(SelectionEvent evt) {
    	for( MSelectionObserver obs : selectionObservers) {
    		obs.selectionChanged(evt);
    	}
    }
    void triggerBuildingSelection(SelectionEvent evt) {
    	for( MSelectionObserver obs : selectionObservers) {
    		obs.buildingSelection(evt);
    	}
    }

    public void addSelectionObserver( MSelectionObserver obs) { selectionObservers.add(obs);}
	public void removeSelectionObserver( MSelectionObserver obs) { selectionObservers.remove(obs); }
	    
}
