package spirite.base.util;

/**
 * The purpose of this class is to streamline the binding of UI to Data in which
 * both are listening for each other thus need to be locked out of endless loops
 */
public class DataBinding {
	private boolean uiLock = false;
	private boolean dataLock = false;
	DBSub link;
	
	public void uiChange( Object newValue) {
		if( uiLock )
			return;
		uiLock = true;
		if( link != null)
			link.doUIChange( newValue);
		uiLock = false;
	}
	public void dataChange( Object newValue) {
		if( dataLock)
			return;
		dataLock = true;
		if( link != null)
			link.doDataChange(newValue);
		dataLock = false;
	}
	public void setLink( DBSub sub) {this.link = sub;}

	public interface DBSub {
		public void doUIChange( Object newValue);
		public void doDataChange( Object newValue);	
	}
}
