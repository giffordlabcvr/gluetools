package uk.ac.gla.cvr.gluetools.core.datamodel.variation;

import java.util.LinkedHashMap;
import java.util.Map;

import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._Feature;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._Variation;
import uk.ac.gla.cvr.gluetools.core.transcription.TranscriptionFormat;
import uk.ac.gla.cvr.gluetools.core.transcription.TranscriptionUtils;

@GlueDataClass(defaultListColumns = {_Variation.NAME_PROPERTY, Variation.TRANSCRIPTION_TYPE_PROPERTY, Variation.REGEX_PROPERTY, _Variation.DESCRIPTION_PROPERTY})
public class Variation extends _Variation {

	private TranscriptionFormat transcriptionFormat;

	public static final String FEATURE_NAME_PATH = _Variation.FEATURE_PROPERTY+"."+_Feature.NAME_PROPERTY;

	public static Map<String, String> pkMap(String featureName, String name) {
		Map<String, String> idMap = new LinkedHashMap<String, String>();
		idMap.put(FEATURE_NAME_PATH, featureName);
		idMap.put(NAME_PROPERTY, name);
		return idMap;
	}
	
	@Override
	public void setPKValues(Map<String, String> pkMap) {
		setName(pkMap.get(NAME_PROPERTY));
	}

	@Override
	protected Map<String, String> pkMap() {
		return pkMap(getFeature().getName(), getName());
	}
	
	public TranscriptionFormat getTranscriptionFormat() {
		if(transcriptionFormat == null) {
			transcriptionFormat = buildTranscriptionFormat();
		}
		return transcriptionFormat;
	}
	
	private TranscriptionFormat buildTranscriptionFormat() {
		return TranscriptionUtils.transcriptionFormatFromString(getTranscriptionType());
	}	

	
}