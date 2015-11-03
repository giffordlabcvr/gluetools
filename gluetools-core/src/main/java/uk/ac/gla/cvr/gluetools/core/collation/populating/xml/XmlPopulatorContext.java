package uk.ac.gla.cvr.gluetools.core.collation.populating.xml;

import java.util.LinkedHashMap;
import java.util.Map;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;

public class XmlPopulatorContext {

	private CommandContext cmdContext;
	private Map<String, Object> fieldUpdates = new LinkedHashMap<String, Object>();
	
	public XmlPopulatorContext(CommandContext cmdContext) {
		super();
		this.cmdContext = cmdContext;
	}

	public CommandContext getCmdContext() {
		return cmdContext;
	}

	public Map<String, Object> getFieldUpdates() {
		return fieldUpdates;
	}
	
	
	
}
