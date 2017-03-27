package spirite.image_data;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import spirite.image_data.GroupTree.Node;
import spirite.image_data.layers.Layer;

/**
 * ReferenceManager manages the Reference system.
 *
 *	References Come in two types: 
 *	-Layers dragged from the Workspace, they are affected by the Reference transform.
 *	-Floating Images pasted into the floating reference section.
 */
public class ReferenceManager {
	private boolean editingReference = false;
	private float refAlpha = 1.0f;

	AffineTransform globalTransform = new AffineTransform();
	AffineTransform calcTransform = new AffineTransform();	// just for caching purposes

	// null signifies the Workspace layer
	private final List<Reference> references = new ArrayList<>();
	
	ReferenceManager(ImageWorkspace imageWorkspace) {
		references.add(null);
	}
	
	// ==================
	// ==== Reference Types
	public abstract class Reference {
		protected boolean global;
		AffineTransform localTransform = new AffineTransform();
		public abstract void draw( Graphics g);
		public final boolean isGlobal() {return global;}
	}
	
	public class LayerReference extends Reference{
		public final Layer layer;
		
		private LayerReference( Layer layer) {
			this.layer = layer;
			this.global = true;
		}

		@Override
		public void draw(Graphics g) {
			Graphics2D g2 = (Graphics2D)g;
			AffineTransform t = g2.getTransform();
			g2.transform(localTransform);
			
			layer.draw(g2);
			
			g2.setTransform(t);
		}
	}
	public class ImageReference extends Reference {
		public final BufferedImage image;
		ImageReference( BufferedImage image) {
			this.image = image;
			this.global = false;
		}
		@Override
		public void draw(Graphics g) {
			Graphics2D g2 = (Graphics2D)g;
			AffineTransform t = g2.getTransform();
			g2.transform(localTransform);
			
			g2.drawImage( image, 0, 0, null);
			
			g2.setTransform(t);	
		}
	}
	
	// ===========
	// ==== Transform Manipulation
	public void shiftTransform( float dx, float dy) {
		calcTransform.setToIdentity();
		calcTransform.translate(dx, dy);
		globalTransform.preConcatenate(calcTransform);
		triggerReferenceStructureChanged(false);
	}
	
	public void rotateTransform( float theta, float x, float y) {
		calcTransform.setToIdentity();
		calcTransform.translate(x, y);
		calcTransform.rotate( theta);
		calcTransform.translate(-x, -y);
		globalTransform.preConcatenate(calcTransform);
		triggerReferenceStructureChanged(false);
	}
	public void resetTransform() {
		globalTransform.setToIdentity();
		triggerReferenceStructureChanged(false);
	}
	public void zoomTransform( float zoom, float x, float y) {
		calcTransform.setToIdentity();
		calcTransform.translate(x, y);
		calcTransform.scale(zoom, zoom);
		calcTransform.translate(-x, -y);
		globalTransform.preConcatenate(calcTransform);
		triggerReferenceStructureChanged(false);
	}
	public AffineTransform getTransform() {
		return new AffineTransform(globalTransform);
	}
	
	
	
	
	// ===============
	// ==== Simple API
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
	public boolean isReferenceNode( Node node) {
		return ( node != null && references.contains(node));
	}
	
	public List<Reference> getList( boolean front) {
		if( front)
			return new ArrayList<Reference>(references.subList(0, references.indexOf(null)));
		else
			return new ArrayList<Reference>(references.subList(references.indexOf(null)+1, references.size()));
	}

	public List<ImageHandle> getDependencies( boolean front) {
		List<Reference> refs = getList(front);
		List<ImageHandle> dependencies = new ArrayList<>();
		
		for( Reference ref : refs) {
			if( ref instanceof LayerReference) {
				dependencies.addAll( ((LayerReference) ref).layer.getImageDependencies());
			}
		}
		return dependencies;
	}
	
	public int getCount() {
		return references.size();
	}
	public int getCenter() {
		return references.indexOf(null);
	}
	
	
	// ============
	// ==== Add/Remove References

	public void addReference( Layer toAdd,int index) {
		if( toAdd != null) {
			references.add(index, new LayerReference(toAdd));
			triggerReferenceStructureChanged(true);
		}
	}
	public void addReference( BufferedImage toAdd, int index, AffineTransform local) {
		if( toAdd != null) {
			Reference ref = new ImageReference(toAdd);
			ref.localTransform = local;
			references.add(index, ref);
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
		Reference toMove = references.get(oldIndex);
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
