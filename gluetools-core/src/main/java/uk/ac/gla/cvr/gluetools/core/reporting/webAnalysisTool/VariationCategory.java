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
package uk.ac.gla.cvr.gluetools.core.reporting.webAnalysisTool;

import java.util.List;
import java.util.Optional;

import org.apache.cayenne.exp.Expression;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.plugins.Plugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

public class VariationCategory implements Plugin {

	public static final String NAME = "name";
	public static final String DISPLAY_NAME = "displayName";
	public static final String WHERE_CLAUSE = "whereClause";
	public static final String DESCRIPTION = "description";
	public static final String SELECTED_BY_DEFAULT = "selectedByDefault";
	public static final String OBJECT_RENDERER_MODULE = "objectRendererModule";
	public static final String REPORT_ABSENCE = "reportAbsence";
	// this is a property path defined relative to a variation.
	// if this is defined, for each variation in the category, the value of this property 
	// must be equal to one of the clade names in the query sequence 
	// typing result (e.g. AL_MASTER, AL_1, AL_1a) etc. otherwise the query will not be scanned for that variation.
	public static final String CLADE_MATCH_PROPERTY = "cladeMatchProperty";
	
	private String name;
	private String displayName;
	private Expression whereClause;
	private String description;
	private String objectRendererModule;
	private Boolean selectedByDefault;
	private Boolean reportAbsence;
	private List<String> cladeMatchProperties;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		name = PluginUtils.configureStringProperty(configElem, NAME, true);
		displayName = Optional.ofNullable(PluginUtils.configureStringProperty(configElem, DISPLAY_NAME, false)).orElse(name);
		whereClause = PluginUtils.configureCayenneExpressionProperty(configElem, WHERE_CLAUSE, true);
		description = PluginUtils.configureStringProperty(configElem, DESCRIPTION, false);
		selectedByDefault = Optional.ofNullable(PluginUtils.configureBooleanProperty(configElem, SELECTED_BY_DEFAULT, false)).orElse(false);
		objectRendererModule = PluginUtils.configureStringProperty(configElem, OBJECT_RENDERER_MODULE, true);
		reportAbsence = Optional.ofNullable(PluginUtils.configureBooleanProperty(configElem, REPORT_ABSENCE, false)).orElse(false);
		cladeMatchProperties = PluginUtils.configureStringsProperty(configElem, CLADE_MATCH_PROPERTY);
	}

	public String getName() {
		return name;
	}

	public String getDisplayName() {
		return displayName;
	}

	public Expression getWhereClause() {
		return whereClause;
	}

	public String getDescription() {
		return description;
	}

	public Boolean getSelectedByDefault() {
		return selectedByDefault;
	}

	public Boolean getReportAbsence() {
		return reportAbsence;
	}

	public String getObjectRendererModule() {
		return objectRendererModule;
	}

	public List<String> getCladeMatchProperties() {
		return cladeMatchProperties;
	}

}
