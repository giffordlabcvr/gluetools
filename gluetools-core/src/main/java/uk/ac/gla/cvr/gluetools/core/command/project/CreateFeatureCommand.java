package uk.ac.gla.cvr.gluetools.core.command.project;

import java.util.Optional;

import org.apache.cayenne.ObjectContext;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.result.CreateResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.feature.Feature;
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
		ObjectContext objContext = cmdContext.getObjectContext();
		Feature feature = GlueDataObject.create(objContext, 
				Feature.class, Feature.pkMap(featureName), false);
		description.ifPresent(d -> {feature.setDescription(d);});
		parentName.ifPresent(pname -> {
			Feature parentFeature = GlueDataObject.lookup(objContext, Feature.class, Feature.pkMap(pname));
			feature.setParent(parentFeature);
		});
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
