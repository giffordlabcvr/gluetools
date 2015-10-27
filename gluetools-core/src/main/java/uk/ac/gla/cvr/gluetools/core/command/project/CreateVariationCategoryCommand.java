package uk.ac.gla.cvr.gluetools.core.command.project;

import java.util.Optional;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.result.CreateResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.variationCategory.VariationCategory;
import uk.ac.gla.cvr.gluetools.core.datamodel.variationCategory.VariationCategory.NotifiabilityLevel;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass( 
	commandWords={"create","variation-category"}, 
	docoptUsages={"<name> [-p <parentName>] [-n <notifiabilityLevel>] [<description>]"},
	docoptOptions={"-p <parentName>, --parentName <parentName>  Name of parent category", 
			"-n <level>, --notifiabilityLevel <level>  Possible values: [NOTIFIABLE, NOT_NOTIFIABLE]"},
	metaTags={CmdMeta.updatesDatabase},
	description="Create a new variation category", 
	furtherHelp="A variation category is a grouping of variations with shared characteristics. Variation categories may be arranged into a parent-child hierarchy") 
public class CreateVariationCategoryCommand extends ProjectModeCommand<CreateResult> {

	public static final String NAME = "name";
	public static final String PARENT_NAME = "parentName";
	public static final String DESCRIPTION = "description";
	public static final String NOTIFIABILITY_LEVEL = "notifiabilityLevel";

	private String variationCategoryName;
	private Optional<String> description;
	private Optional<String> parentName;
	private NotifiabilityLevel notifiabilityLevel;

	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		variationCategoryName = PluginUtils.configureStringProperty(configElem, NAME, true);
		parentName = Optional.ofNullable(PluginUtils.configureStringProperty(configElem, PARENT_NAME, false));
		description = Optional.ofNullable(PluginUtils.configureStringProperty(configElem, DESCRIPTION, false));
		notifiabilityLevel = Optional.ofNullable(
				PluginUtils.configureEnumProperty(NotifiabilityLevel.class, configElem, NOTIFIABILITY_LEVEL, false)).
				orElse(NotifiabilityLevel.NOTIFIABLE);

	}

	@Override
	public CreateResult execute(CommandContext cmdContext) {
		
		VariationCategory variationCategory = GlueDataObject.create(cmdContext, 
				VariationCategory.class, VariationCategory.pkMap(variationCategoryName), false);
		variationCategory.setNotifiability(notifiabilityLevel.name());

		description.ifPresent(d -> {variationCategory.setDescription(d);});
		parentName.ifPresent(pname -> {
			VariationCategory parentVariationCategory = GlueDataObject.lookup(cmdContext, VariationCategory.class, VariationCategory.pkMap(pname));
			variationCategory.setParent(parentVariationCategory);
		});
		cmdContext.commit();
		return new CreateResult(VariationCategory.class, 1);
	}

	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerDataObjectNameLookup("parentName", VariationCategory.class, VariationCategory.NAME_PROPERTY);
			registerEnumLookup("notifiabilityLevel", NotifiabilityLevel.class);
		}
	}
	
}
