package uk.ac.gla.cvr.gluetools.core.datamodel.projectSetting;

import java.util.LinkedHashMap;
import java.util.Map;

import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._ProjectSetting;

@GlueDataClass(defaultListedFields = {ProjectSetting.NAME_PROPERTY, ProjectSetting.VALUE_PROPERTY})
public class ProjectSetting extends _ProjectSetting {

	private ProjectSettingOption option;
	
	public ProjectSettingOption getProjectSettingOption() {
		if(option == null) {
			option = buildProjectSettingOption();
		}
		return option;
	}
	
	private ProjectSettingOption buildProjectSettingOption() {
		String name = getName();
		try {
			return ProjectSettingOption.valueOf(name);
		} catch(IllegalArgumentException iae) {
			throw new ProjectSettingException(ProjectSettingException.Code.UNKNOWN_SETTING, name);
		}
	}
	
	public static Map<String, String> pkMap(String name) {
		Map<String, String> idMap = new LinkedHashMap<String, String>();
		idMap.put(NAME_PROPERTY, name);
		return idMap;
	}
	
	
	
	@Override
	public void setName(String name) {
		super.setName(name);
		this.option = null;
	}

	@Override
	public void setPKValues(Map<String, String> pkMap) {
		setName(pkMap.get(NAME_PROPERTY));
	}

	@Override
	public Map<String, String> pkMap() {
		return pkMap(getName());
	}

}
