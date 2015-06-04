package uk.ac.gla.cvr.gluetools.core.collation.sequence;

import java.util.LinkedHashMap;
import java.util.Map;

import org.w3c.dom.Document;

import uk.ac.gla.cvr.gluetools.core.datafield.DataField;
import uk.ac.gla.cvr.gluetools.core.datafield.DataFieldValue;
import uk.ac.gla.cvr.gluetools.core.datafield.populator.DataFieldPopulatorException;
import uk.ac.gla.cvr.gluetools.core.datafield.populator.DataFieldPopulatorException.Code;
import uk.ac.gla.cvr.gluetools.core.project.Project;



/** 
 * A single sequence which belongs to a particular project.
 * The sequence will have been obtained from a sequence source by a SequenceSourcer.
 * The sequenceSourceID is the ID of this sequence within the source. 
 * The sourceUniqueID uniquely identifies the source.
 * Therefore the (sequenceSourceID, sourceUniqueID) as a pair uniquely identify this Collated sequence.
 * Collated sequences can be in different formats. The format field specifies which, and the sequence text field contains the
 * sequence itself in the specified format.
 * Collated sequences may have values for any of the fields in the owning project.
 */
public class CollatedSequence {

	private Project owningProject;
	
	private Map<String, DataFieldValue<?>> dataFieldValues = new LinkedHashMap<String, DataFieldValue<?>>();
	
	private String sequenceSourceID;
	
	private String sourceUniqueID;
	
	private String sequenceText;
	
	private Document cachedXml;
	
	private CollatedSequenceFormat format;

	public String getSequenceSourceID() {
		return sequenceSourceID;
	}

	public void setSequenceSourceID(String sequenceSourceID) {
		this.sequenceSourceID = sequenceSourceID;
	}

	public String getSourceUniqueID() {
		return sourceUniqueID;
	}

	public void setSourceUniqueID(String sourceUniqueID) {
		this.sourceUniqueID = sourceUniqueID;
	}

	public String getSequenceText() {
		return sequenceText;
	}

	public void setSequenceText(String sequenceText) {
		this.sequenceText = sequenceText;
		this.cachedXml = null;
	}

	public CollatedSequenceFormat getFormat() {
		return format;
	}

	public void setFormat(CollatedSequenceFormat format) {
		this.format = format;
	}
	
	public void setDataFieldValue(String name, String valueAsString) {
		DataField<?> dataField = owningProject.getDataField(name);
		if(dataField == null) {
			throw new DataFieldPopulatorException(Code.NO_SUCH_FIELD, name, owningProject.getID());
		}
		dataFieldValues.put(name, dataField.valueFromString(valueAsString));
	}

	public Document asXml() {
		if(cachedXml == null) {
			cachedXml = getFormat().asXml(getSequenceText());
		}
		return cachedXml;
	}

	public Project getOwningProject() {
		return owningProject;
	}

	public void setOwningProject(Project owningProject) {
		this.owningProject = owningProject;
	}

	public DataFieldValue<?> getDataFieldValue(String name) {
		return dataFieldValues.get(name);
	}
	
}
