package uk.ac.gla.cvr.gluetools.core.collation.populating.xml;

import java.util.LinkedHashMap;
import java.util.Map;

import uk.ac.gla.cvr.gluetools.core.collation.populating.SequencePopulator.FieldUpdate;
import uk.ac.gla.cvr.gluetools.core.datamodel.field.FieldType;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;

public class XmlPopulatorContext {

	private Sequence sequence;
	private Map<String, FieldUpdate> fieldUpdates = new LinkedHashMap<String, FieldUpdate>();
	private Map<String, FieldType> fieldTypes;
	
	public XmlPopulatorContext(Sequence sequence, Map<String, FieldType> fieldTypes) {
		super();
		this.sequence = sequence;
		this.fieldTypes = fieldTypes;
	}

	public boolean isAllowedField(String fieldName) {
		return fieldTypes.containsKey(fieldName);
	}

	public Map<String, FieldUpdate> getFieldUpdates() {
		return fieldUpdates;
	}
	
	public FieldType getFieldType(String fieldName) {
		return fieldTypes.get(fieldName);
	}

	public Sequence getSequence() {
		return sequence;
	}
	
	
}
