package uk.ac.gla.cvr.gluetools.core.command;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.plugins.Plugin;

public abstract class Command<R extends CommandResult> implements Plugin {

	/** this property name must always be used for binary input in base64 form */
	public static final String BINARY_INPUT_PROPERTY = "base64";

	/** this property name must always be used for binary output in base64 form */
	public static final String BINARY_OUTPUT_PROPERTY = "base64";

	private Element cmdElem;
	
	public Element getCmdElem() {
		return cmdElem;
	}

	public void setCmdElem(Element cmdElem) {
		this.cmdElem = cmdElem;
	}

	public abstract R execute(CommandContext cmdContext);

}
