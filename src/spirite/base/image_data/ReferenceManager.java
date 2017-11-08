package spirite.base.image_data;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import spirite.base.graphics.GraphicsContext;
import spirite.base.graphics.IImage;
import spirite.base.image_data.GroupTree.LayerNode;
import spirite.base.image_data.GroupTree.Node;
import spirite.base.image_data.layers.Layer;
import spirite.base.util.ObserverHandler;
import spirite.base.util.glmath.MatTrans;

/**
 * ReferenceManager manages the Reference system.
 *
 *	References Come in two types: 
 *	-Layers dragged from the Workspace, they are affected by the Reference transform.
 *	-Floating Images pasted into the floating reference section.
 */
public class ReferenceManager {
	private final ImageWorkspace context;
	private boolean editingReference = false;
	private float refAlpha = 1.0f;

	MatTrans globalTransform = new MatTrans();
	MatTrans calcTransform = new MatTrans();	// just for caching purposes

	// null signifies the Workspace layer
	private final List<Reference> references = new ArrayList<>();
	
	ReferenceManager(ImageWorkspace imageWorkspace) {
		this.context = imageWorkspace;
		references.add(null);
	}
	
	// ==================
	// ==== Reference Types
	public abstract class Reference {
		protected boolean global;
		MatTrans localTransform = new MatTrans();
		public abstract void draw( GraphicsContext gc);
		public final boolean isGlobal() {return global;}
	}
	
	public class LayerReference extends Reference{
		public final Layer layer;
		
		private LayerReference( Layer layer) {
			this.layer = layer;
			this.global = true;
		}

		@Override
		public void draw(GraphicsContext gc) {
			MatTrans oldTrans = gc.getTransform();
			MatTrans newTrans = new MatTrans(oldTrans);
			if( global) 
				newTrans.concatenate(getTransform());
			newTrans.concatenate(localTransform);
			gc.setTransform(newTrans);
			
			layer.draw(gc);
			 
			gc.setTransform(oldTrans);
		}
	}
	public class ImageReference extends Reference {
		public final IImage image;
		ImageReference( IImage iImage) {
			this.image = iImage;
			this.global = false;
		}
		@Override
		public void draw(GraphicsContext gc) {
			MatTrans oldTrans = gc.getTransform();
			MatTrans newTrans = new MatTrans(oldTrans);
			if( global) 
				newTrans.concatenate(getTransform());
			newTrans.concatenate(localTransform);
			gc.setTransform(newTrans);
			
			gc.drawImage( image, 0, 0);

			gc.setTransform(oldTrans);
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
	public MatTrans getTransform() {
		return new MatTrans(globalTransform);
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
		if( node == null || !(node instanceof LayerNode)) return false;
		
		for( Reference reference : references) {
			if( reference instanceof LayerReference && ((LayerReference) reference).layer == ((LayerNode)node).getLayer())
				return true;
		}
		return false;
	}
	
	public List<Reference> getList( boolean front) {
		if( front)
			return new ArrayList<Reference>(references.subList(0, references.indexOf(null)));
		else
			return new ArrayList<Reference>(references.subList(references.indexOf(null)+1, references.size()));
	}

	public List<MediumHandle> getDependencies( boolean front) {
		List<Reference> refs = getList(front);
		List<MediumHandle> dependencies = new ArrayList<>();
		
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
	public void addReference( IImage iImage, int index, MatTrans local) {
		if( iImage != null) {
			Reference ref = new ImageReference(iImage);
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
		if( toRem == null) return;
		
		Iterator<Reference> it = references.iterator();
		while( it.hasNext()) {
			Reference reference = it.next();

			if( reference instanceof LayerReference && ((LayerReference) reference).layer == toRem)
				it.remove();
		}
	}
	
	

    /**
     * MReferenceObservers - Triggers when the reference structure has changed,
     * when the way References are drawn is changed, or when the input method
     * is toggled between reference of non-reference.
     */
	private final ObserverHandler<MReferenceObserver> referenceObs = new ObserverHandler<>();
    public void addReferenceObserve( MReferenceObserver obs) { referenceObs.addObserver(obs);}
    public void removeReferenceObserve( MReferenceObserver obs) { referenceObs.removeObserver(obs);}
	
    public static interface MReferenceObserver {
    	public void referenceStructureChanged( boolean hard);
//    	public void referenceImageChanged();
    	public void toggleReference( boolean referenceMode);
    }

    public void triggerReferenceStructureChanged(boolean hard) {
    	referenceObs.trigger((MReferenceObserver obs) -> {obs.referenceStructureChanged(hard);});
    	context.triggerFlash();
    }
    private void triggerReferenceToggle(boolean edit) {
    	referenceObs.trigger((MReferenceObserver obs) -> {obs.toggleReference(edit);});
    	context.triggerFlash();
    }


}
