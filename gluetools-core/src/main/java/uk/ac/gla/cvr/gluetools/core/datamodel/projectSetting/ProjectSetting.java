/**
 *    GLUE: A flexible system for virus sequence data
 *    Copyright (C) 2018 The University of Glasgow
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Affero General Public License as published
 *    by the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Affero General Public License for more details.

 *    You should have received a copy of the GNU Affero General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *    Contact details:
 *    MRC-University of Glasgow Centre for Virus Research
 *    Sir Michael Stoker Building, Garscube Campus, 464 Bearsden Road, 
 *    Glasgow G61 1QH, United Kingdom
 *    
 *    Josh Singer: josh.singer@glasgow.ac.uk
 *    Rob Gifford: robert.gifford@glasgow.ac.uk
*/
package uk.ac.gla.cvr.gluetools.core.datamodel.projectSetting;

import java.util.LinkedHashMap;
import java.util.Map;

import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._ProjectSetting;

@GlueDataClass(defaultListedProperties = {ProjectSetting.NAME_PROPERTY, ProjectSetting.VALUE_PROPERTY})
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
