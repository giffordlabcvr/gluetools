package uk.ac.gla.cvr.gluetools.core.command.project;

import java.util.List;
import java.util.Optional;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.SelectQuery;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.result.CreateResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.feature.Feature;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureMetatag.FeatureMetatag;
import uk.ac.gla.cvr.gluetools.core.datamodel.projectSetting.ProjectSettingOption;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass( 
	commandWords={"create","feature"}, 
	docoptUsages={"<featureName> [-p <parent>] [<description>]"},
	docoptOptions={"-p <featureName>, --parentName <featureName>  Name of parent feature"},
	metaTags={CmdMeta.updatesDatabase},
	description="Create a new genome feature", 
	furtherHelp="A feature is a named genome region which is of particular interest.") 
public class CreateFeatureCommand extends ProjectModeCommand<CreateResult> {

	public static final String FEATURE_NAME = "featureName";
	public static final String PARENT_NAME = "parentName";
	public static final String DESCRIPTION = "description";
	
	private String featureName;
	private Optional<String> description;
	private Optional<String> parentName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		featureName = PluginUtils.configureStringProperty(configElem, FEATURE_NAME, true);
		parentName = Optional.ofNullable(PluginUtils.configureStringProperty(configElem, PARENT_NAME, false));
		description = Optional.ofNullable(PluginUtils.configureStringProperty(configElem, DESCRIPTION, false));
	}

	@Override
	public CreateResult execute(CommandContext cmdContext) {
		
		Feature feature = GlueDataObject.create(cmdContext, 
				Feature.class, Feature.pkMap(featureName), false);
		description.ifPresent(d -> {feature.setDescription(d);});
		parentName.ifPresent(pname -> {
			Feature parentFeature = GlueDataObject.lookup(cmdContext, Feature.class, Feature.pkMap(pname));
			feature.setParent(parentFeature);
		});
		String inferOrderValue = cmdContext.getProjectSettingValue(ProjectSettingOption.INFER_FEATURE_DISPLAY_ORDER);
		if(inferOrderValue.equals("true")) {
			List<Feature> featuresWithSameParent;
			Feature parent = feature.getParent();
			if(parent != null) {
				featuresWithSameParent = parent.getChildren();
			} else {
				Expression exp = ExpressionFactory.matchExp(Feature.PARENT_PROPERTY, null);
				featuresWithSameParent = GlueDataObject.query(cmdContext, Feature.class, new SelectQuery(Feature.class, exp));
			}
			int maxOrderNumber = -1;
			for(Feature sibling: featuresWithSameParent) {
				Integer siblingDisplayOrder = sibling.getDisplayOrder();
				if(siblingDisplayOrder != null) {
					maxOrderNumber = Math.max(maxOrderNumber, siblingDisplayOrder);
				}
			}
			FeatureMetatag displayOrderMetatag = GlueDataObject.create(cmdContext, 
					FeatureMetatag.class, FeatureMetatag.pkMap(featureName, FeatureMetatag.Type.DISPLAY_ORDER.name()), false);
			displayOrderMetatag.setFeature(feature);
			displayOrderMetatag.setValue(Integer.toString(maxOrderNumber+1));
		}
		cmdContext.commit();
		return new CreateResult(Feature.class, 1);
	}

	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerDataObjectNameLookup("parent", Feature.class, Feature.NAME_PROPERTY);
		}
	}
	
}
