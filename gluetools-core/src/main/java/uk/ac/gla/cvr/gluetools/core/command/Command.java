package uk.ac.gla.cvr.gluetools.core.command;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.plugins.Plugin;

public abstract class Command<R extends CommandResult> implements Plugin {

	private Element cmdElem;
	
	public Element getCmdElem() {
		return cmdElem;
	}

	public void setCmdElem(Element cmdElem) {
		this.cmdElem = cmdElem;
	}

	public abstract R execute(CommandContext cmdContext);
	
}
