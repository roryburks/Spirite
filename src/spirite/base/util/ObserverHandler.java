package spirite.base.util;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ObserverHandler<T>
{
	public interface Doer<T> {
		public void Do(T t);
	}
	
	private final List<WeakReference<T>> observers = new ArrayList<>();
	
	public void addObserver(T toAdd) {observers.add(new WeakReference<>(toAdd));}
	
	public void removeObserver( T toRemove) {
    	Iterator<WeakReference<T>> it = observers.iterator();
    	while( it.hasNext()) {
    		T other = it.next().get();
    		if( other == toRemove || other == null)
    			it.remove();
    	}
	}
	
	public void trigger( Doer<T> doer) {
    	Iterator<WeakReference<T>> it = observers.iterator();
    	while( it.hasNext()) {
    		T other = it.next().get();
    		if( other == null)
    			it.remove();
    		else 
    			doer.Do(other);
    	}
	}
}