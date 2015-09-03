package uk.ac.gla.cvr.gluetools.core.datamodel.feature;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._Feature;
import uk.ac.gla.cvr.gluetools.core.transcription.TranscriptionFormat;
import uk.ac.gla.cvr.gluetools.core.transcription.TranscriptionUtils;

@GlueDataClass(defaultListColumns = {_Feature.NAME_PROPERTY, _Feature.TRANSCRIPTION_TYPE_PROPERTY, Feature.PARENT_NAME_PATH, _Feature.DESCRIPTION_PROPERTY})
public class Feature extends _Feature {

	public static final String PARENT_NAME_PATH = _Feature.PARENT_PROPERTY+"."+_Feature.NAME_PROPERTY;

	
	private TranscriptionFormat transcriptionFormat;

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

	public TranscriptionFormat getTranscriptionFormat() {
		if(transcriptionFormat == null) {
			transcriptionFormat = buildTranscriptionFormat();
		}
		return transcriptionFormat;
	}
	
	private TranscriptionFormat buildTranscriptionFormat() {
		return TranscriptionUtils.transcriptionFormatFromString(getTranscriptionType());
	}	

	
	@Override
	public void setParent(Feature parent) {
		if(parent != null) {
			Set<String> loopFeatureNames = new LinkedHashSet<String>();
			loopFeatureNames.add(this.getName());
			Feature current = parent;
			while(current != null) {
				String currentName = current.getName();
				if(loopFeatureNames.contains(currentName)) {
					List<String> loopNames = new ArrayList<String>(loopFeatureNames);
					loopNames.add(currentName);
					throw new FeatureException(FeatureException.Code.PARENT_RELATIONSHIP_LOOP, loopNames);
				} else {
					loopFeatureNames.add(currentName);
					current = current.getParent();
				}
			}
		}
		super.setParent(parent);
	}

	
	
}
