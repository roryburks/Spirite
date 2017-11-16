package spirite.base.brains.commands;

import java.util.List;

public interface CommandExecuter {
	public abstract List<String> getValidCommands();
	public String getCommandDomain();
	public boolean executeCommand( String command, Object extra);
}