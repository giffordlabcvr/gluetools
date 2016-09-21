package uk.ac.gla.cvr.gluetools.core.document;

import uk.ac.gla.cvr.gluetools.utils.GlueTypeUtils.GlueType;

public class SimpleCommandValue extends CommandValue implements CommandFieldValue, CommandArrayItem {

	private GlueType glueType;
	private Object value;

	public SimpleCommandValue(GlueType glueType, Object value) {
		super();
		this.glueType = glueType;
		this.value = value;
	}

	public Object getValue() {
		return value;
	}
	
	public GlueType getGlueType() {
		return glueType;
	}

}
