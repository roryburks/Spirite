package spirite.base.util;

/**
 * The purpose of this class is to streamline the binding of UI to Data in which
 * both are listening for each other thus need to be locked out of endless loops
 */
public class DataBinding<T> {
	private boolean lock = false;
	ChangeExecuter<T> link;
	
	public void triggerUIChanged( T newValue) {
		if( lock )
			return;
		lock = true;
		if( link != null)
			link.doUIChanged( newValue);
		lock = false;
	}
	public void triggerDataChanged( T newValue) {
		if( lock)
			return;
		lock = true;
		if( link != null)
			link.doDataChanged(newValue);
		lock = false;
	}
	public void setLink( ChangeExecuter<T> executer) {this.link = executer;}

	public interface ChangeExecuter<T> {
		public void doUIChanged( T newValue);
		public void doDataChanged( T newValue);	
	}
}
