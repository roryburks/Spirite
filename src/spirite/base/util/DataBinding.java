package spirite.base.util;

/**
 * The purpose of this class is to streamline the binding of UI to Data in which
 * both are listening for each other thus need to be locked out of endless loops
 */
public class DataBinding {
	private boolean lock = false;
	DBSub link;
	
	public void uiChange( Object newValue) {
		System.out.println("UIChangeStart");
		if( lock )
			return;
		lock = true;
		if( link != null)
			link.doUIChange( newValue);
		lock = false;
		System.out.println("UIChangeEnd");
	}
	public void dataChange( Object newValue) {
		System.out.println("DataChangeStart");
		if( lock)
			return;
		lock = true;
		if( link != null)
			link.doDataChange(newValue);
		lock = false;
		System.out.println("DataChangeEnd");
	}
	public void setLink( DBSub sub) {this.link = sub;}

	public interface DBSub {
		public void doUIChange( Object newValue);
		public void doDataChange( Object newValue);	
	}
}
