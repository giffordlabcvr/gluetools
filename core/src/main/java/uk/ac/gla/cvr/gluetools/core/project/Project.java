package uk.ac.gla.cvr.gluetools.core.project;

import java.util.LinkedHashMap;
import java.util.Map;

import uk.ac.gla.cvr.gluetools.core.datafield.DataField;

public class Project {

	private String id;
	
	private String displayName;
	
	private Map<String, DataField<?>> dataFields = new LinkedHashMap<String, DataField<?>>();
	
	public DataField<?> getDataField(String name) {
		return dataFields.get(name);
	}

	public boolean hasDataField(String name) {
		return dataFields.containsKey(name);
	}

	public String getDisplayName() {
		return displayName;
	}
	
	public String getID() {
		return id;
	}

	public void addDataField(DataField<?> dataField) {
		dataFields.put(dataField.getName(), dataField);
	}
}
