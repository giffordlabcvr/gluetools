package uk.ac.gla.cvr.gluetools.core.reporting.webAnalysisTool;

import java.util.List;
import java.util.Optional;

import org.apache.cayenne.exp.Expression;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.plugins.Plugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;

public class VariationCategory implements Plugin {

	public static final String NAME = "name";
	public static final String DISPLAY_NAME = "displayName";
	public static final String WHERE_CLAUSE = "whereClause";
	public static final String DESCRIPTION = "description";
	public static final String SELECTED_BY_DEFAULT = "selectedByDefault";
	public static final String PROPERTY_TEMPLATE = "propertyTemplate";
	
	private String name;
	private String displayName;
	private Expression whereClause;
	private String description;
	private Boolean selectedByDefault;
	private List<PropertyTemplate> propertyTemplates;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		name = PluginUtils.configureStringProperty(configElem, NAME, true);
		displayName = Optional.ofNullable(PluginUtils.configureStringProperty(configElem, DISPLAY_NAME, false)).orElse(name);
		whereClause = PluginUtils.configureCayenneExpressionProperty(configElem, WHERE_CLAUSE, true);
		description = PluginUtils.configureStringProperty(configElem, DESCRIPTION, false);
		selectedByDefault = Optional.ofNullable(PluginUtils.configureBooleanProperty(configElem, SELECTED_BY_DEFAULT, false)).orElse(false);
		propertyTemplates = 
				PluginFactory.createPlugins(pluginConfigContext, PropertyTemplate.class, 
						GlueXmlUtils.getXPathElements(configElem, PROPERTY_TEMPLATE));

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

	public List<PropertyTemplate> getPropertyTemplates() {
		return propertyTemplates;
	}
	
}
