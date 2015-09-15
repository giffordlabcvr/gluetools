package uk.ac.gla.cvr.gluetools.core.datamodel.refSequence;

import java.util.LinkedHashMap;
import java.util.Map;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._Sequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._Source;

@GlueDataClass(defaultListColumns = {_ReferenceSequence.NAME_PROPERTY, ReferenceSequence.SEQ_SOURCE_NAME_PATH, ReferenceSequence.SEQ_ID_PATH})
public class ReferenceSequence extends _ReferenceSequence {

	public static final String SEQ_SOURCE_NAME_PATH = 
			_ReferenceSequence.SEQUENCE_PROPERTY+"."+
					_Sequence.SOURCE_PROPERTY+"."+_Source.NAME_PROPERTY;
	
	public static final String SEQ_ID_PATH = 
			_ReferenceSequence.SEQUENCE_PROPERTY+"."+_Sequence.SEQUENCE_ID_PROPERTY;

	
	public static Map<String, String> pkMap(String name) {
		Map<String, String> idMap = new LinkedHashMap<String, String>();
		idMap.put(NAME_PROPERTY, name);
		return idMap;
	}
	

	@Override
	public void setPKValues(Map<String, String> pkMap) {
		setName(pkMap.get(NAME_PROPERTY));
	}

	@Override
	protected Map<String, String> pkMap() {
		return pkMap(getName());
	}


	public void validate(CommandContext cmdContext) {
		getFeatureLocations().forEach(featureLoc -> featureLoc.validate(cmdContext));

	}


}
