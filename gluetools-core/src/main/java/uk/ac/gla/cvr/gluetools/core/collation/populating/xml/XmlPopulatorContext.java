package uk.ac.gla.cvr.gluetools.core.collation.populating.xml;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;

public class XmlPopulatorContext {

	private CommandContext cmdContext;
	private Set<String> allowedFields = null;
	private Map<String, Object> fieldUpdates = new LinkedHashMap<String, Object>();
	
	public XmlPopulatorContext(CommandContext cmdContext, List<String> fieldNames) {
		super();
		this.cmdContext = cmdContext;
		if(fieldNames != null) {
			allowedFields = new LinkedHashSet<String>(fieldNames);
		}
	}

	public CommandContext getCmdContext() {
		return cmdContext;
	}
	
	public boolean isAllowedField(String fieldName) {
		if(allowedFields == null) {
			return true;
		} else {
			return allowedFields.contains(fieldName);
		}
	}

	public Map<String, Object> getFieldUpdates() {
		return fieldUpdates;
	}
	
	
	
}
