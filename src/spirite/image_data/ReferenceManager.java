package spirite.image_data;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import spirite.image_data.GroupTree.Node;
import spirite.image_data.layers.Layer;

/**
 * ReferenceManager manages the Reference system.
 *
 */
public class ReferenceManager {
	private boolean editingReference = false;
	private float refAlpha = 1.0f;
	
	// null signifies the Workspace layer
	private final List<Layer> references = new ArrayList<>();
	
	ReferenceManager(ImageWorkspace imageWorkspace) {
		references.add(null);
	}
	
	public boolean isEditingReference() {
		return editingReference;
	}
	public void setEditingReference( boolean edit) {
		if( editingReference != edit) {
			editingReference = edit;
			triggerReferenceToggle( edit);
		}
	}
	
	public float getRefAlpha() {return refAlpha;}
	public void setRefAlpha( float alpha) { 
		this.refAlpha = alpha;
		triggerReferenceStructureChanged(false);
	}
	
	
	
	public void addReference( Layer toAdd,int index) {
		if( toAdd != null) {
			references.add(index, toAdd);
			triggerReferenceStructureChanged(true);
		}
	}
	public void removeReferenceAt( int index) {
		if( references.get(index) != null && index < references.size()) {
			references.remove(index);
			triggerReferenceStructureChanged(true);
		}
	}
	public void moveReference( int oldIndex, int newIndex) {
		Layer toMove = references.get(oldIndex);
		if( oldIndex > newIndex) {
			references.remove(oldIndex);
			references.add(newIndex,toMove);
		}
		else {
			references.add(newIndex,toMove);
			references.remove(oldIndex);
		}
		triggerReferenceStructureChanged(true);
	}
	public void clearReference( Layer toRem) {
		if( toRem != null) {
			while( references.contains(toRem))
				references.remove(toRem);
			triggerReferenceStructureChanged(true);
		}
	}
	
	public boolean isReferenceNode( Node node) {
		return ( node != null && references.contains(node));
	}

	public List<Layer> getFrontList() {
		return new ArrayList<Layer>(references.subList(0, references.indexOf(null)));
	}
	public List<Layer> getBackList() {
		return new ArrayList<Layer>(references.subList(references.indexOf(null)+1, references.size()));
	}
	
	public int getCount() {
		return references.size();
	}
	

    /**
     * MReferenceObservers - Triggers when the reference structure has changed,
     * when the way References are drawn is changed, or when the input method
     * is toggled between reference of non-reference.
     */
    public static interface MReferenceObserver {
    	public void referenceStructureChanged( boolean hard);
//    	public void referenceImageChanged();
    	public void toggleReference( boolean referenceMode);
    }
    private final List<WeakReference<MReferenceObserver>> referenceObservers = new ArrayList<>();

    public void triggerReferenceStructureChanged(boolean hard) {
    	Iterator<WeakReference<MReferenceObserver>> it = referenceObservers.iterator();
    	while( it.hasNext()) {
    		MReferenceObserver obs = it.next().get();
    		if( obs == null) it.remove();
    		else
            	obs.referenceStructureChanged( hard);
    	}
    }
    private void triggerReferenceToggle(boolean edit) {
    	Iterator<WeakReference<MReferenceObserver>> it = referenceObservers.iterator();
    	while( it.hasNext()) {
    		MReferenceObserver obs = it.next().get();
    		if( obs == null) it.remove();
    		else
            	obs.toggleReference( edit);
    	}
    }

    public void addReferenceObserve( MReferenceObserver obs) { referenceObservers.add(new WeakReference<MReferenceObserver>(obs));}
    public void removeReferenceObserve( MReferenceObserver obs) { 
    	Iterator<WeakReference<MReferenceObserver>> it = referenceObservers.iterator();
    	while( it.hasNext()) {
    		MReferenceObserver other = it.next().get();
    		if( other == obs || other == null)
    			it.remove();
    	}
    }

}
