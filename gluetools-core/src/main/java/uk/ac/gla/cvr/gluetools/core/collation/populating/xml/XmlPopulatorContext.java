package uk.ac.gla.cvr.gluetools.core.collation.populating.xml;

import java.util.LinkedHashMap;
import java.util.Map;

import uk.ac.gla.cvr.gluetools.core.collation.populating.SequencePopulator.PropertyUpdate;
import uk.ac.gla.cvr.gluetools.core.datamodel.field.FieldType;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;

public class XmlPopulatorContext {

	private Sequence sequence;
	private Map<String, PropertyUpdate> propertyUpdates = new LinkedHashMap<String, PropertyUpdate>();
	private Map<String, FieldType> fieldTypes;
	private Map<String, String> links; // maps link name to custom table name.
	
	public XmlPopulatorContext(Sequence sequence, Map<String, FieldType> fieldTypes, Map<String, String> links) {
		super();
		this.sequence = sequence;
		this.fieldTypes = fieldTypes;
		this.links = links;
	}

	public boolean isAllowedField(String fieldName) {
		return fieldTypes.containsKey(fieldName);
	}

	public boolean isAllowedLink(String linkName) {
		return links.containsKey(linkName);
	}

	public Map<String, PropertyUpdate> getPropertyUpdates() {
		return propertyUpdates;
	}
	
	public FieldType getFieldType(String fieldName) {
		return fieldTypes.get(fieldName);
	}

	public String getCustomTableName(String linkName) {
		return links.get(linkName);
	}

	public Sequence getSequence() {
		return sequence;
	}
	
	
}
