package spirite.base.util;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class Observer {

	// Exists just for copy-paste purposes
	// Change REPLACE -> Something
	// Change replace -> something
	//	Replace actionTriggered and triggerAction
	public static interface MREPLACEObserver {
		public void actionTriggered( MREPLACEEvent evt);
	}
	public static class MREPLACEEvent {
	}
	
    private void triggerAction( MREPLACEEvent evt) {
    	Iterator<WeakReference<MREPLACEObserver>> it = replaceObservers.iterator();
    	while( it.hasNext()) {
    		MREPLACEObserver obs = it.next().get();
    		if( obs == null) it.remove();
    		else  
    			obs.actionTriggered( evt);
    	}
	}
	
	List< WeakReference<MREPLACEObserver>> replaceObservers = new ArrayList<>();
	public void addREPLACEObserver( MREPLACEObserver obs) { replaceObservers.add(new WeakReference<>(obs));}
	public void removeREPLACEObserver( MREPLACEObserver obs) {
    	Iterator<WeakReference<MREPLACEObserver>> it = replaceObservers.iterator();
    	while( it.hasNext()) {
    		MREPLACEObserver other = it.next().get();
    		if( other == obs || other == null)
    			it.remove();
    	}
	}
}
