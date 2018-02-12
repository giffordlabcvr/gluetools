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
package uk.ac.gla.cvr.gluetools.core.collation.populating.propertyPopulator;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import uk.ac.gla.cvr.gluetools.core.collation.populating.propertyPopulator.PropertyPopulator.PropertyPathInfo;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.InsideProjectMode;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ConfigurableTable;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;

public abstract class SequencePopulator<P extends ModulePlugin<P>> extends ModulePlugin<P> {

	public abstract List<String> allUpdatablePropertyPaths();
	
	public static class PropertyUpdate {
		private boolean updated;
		private PropertyPathInfo propertyPathInfo;
		private String value;

		public PropertyUpdate(boolean updated, PropertyPathInfo propertyPathInfo, String value) {
			super();
			this.updated = updated;
			this.propertyPathInfo = propertyPathInfo;
			this.value = value;
		}

		public boolean updated() {
			return updated;
		}

		public String getValue() {
			return value;
		}

		public PropertyPathInfo getPropertyPathInfo() {
			return propertyPathInfo;
		}
	}

	public static Map<String, PropertyPathInfo> getPropertyPathToInfoMap(CommandContext cmdContext, List<String> updatableProperties) {
		Project project = ((InsideProjectMode) cmdContext.peekCommandMode()).getProject();
		Map<String, PropertyPathInfo> propertyPathToInfo = new LinkedHashMap<String, PropertyPathInfo>();
		for(String updatableProperty: updatableProperties) {
			propertyPathToInfo.put(updatableProperty, PropertyPopulator.analysePropertyPath(project, ConfigurableTable.sequence.name(), updatableProperty));
		}
		return propertyPathToInfo;
	}

	
	
}
