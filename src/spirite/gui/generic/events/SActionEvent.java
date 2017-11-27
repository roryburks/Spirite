package spirite.gui.generic.events;

public class SActionEvent {
	public interface SActionListener {
		public void actionPerformed(SActionEvent evt);
	}
	
	public final String actionCommand;
	
	public SActionEvent( String actionCommand) {
		this.actionCommand = actionCommand;
	}
}
