package uk.ac.gla.cvr.gluetools.core.command.project.variationCategory;

import java.util.Optional;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.result.OkResult;
import uk.ac.gla.cvr.gluetools.core.command.result.UpdateResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.variationCategory.VariationCategory;
import uk.ac.gla.cvr.gluetools.core.datamodel.variationCategory.VariationCategory.NotifiabilityLevel;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@CommandClass( 
		commandWords={"set","notifiability"}, 
		docoptUsages={"<notifiabilityLevel>"},
		docoptOptions={},
		metaTags={CmdMeta.updatesDatabase},
		description="Set the variation category's notifiability level", 
		furtherHelp="Possible values for the notifiability level are [NOTIFIABLE, NOT_NOTIFIABLE]") 
public class VariationCategorySetNotifiabilityCommand extends VariationCategoryModeCommand<OkResult> {

	public static final String NOTIFIABILITY_LEVEL = "notifiabilityLevel";
	
	private NotifiabilityLevel notifiabilityLevel;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		notifiabilityLevel = Optional.ofNullable(
				PluginUtils.configureEnumProperty(NotifiabilityLevel.class, configElem, NOTIFIABILITY_LEVEL, false)).
				orElse(NotifiabilityLevel.NOTIFIABLE);
	}

	@Override
	public OkResult execute(CommandContext cmdContext) {
		VariationCategory variationCategory = lookupVariationCategory(cmdContext);
		variationCategory.setNotifiability(notifiabilityLevel.name());
		cmdContext.commit();
		return new UpdateResult(VariationCategory.class, 1);
	}

	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {

		public Completer() {
			super();
			registerEnumLookup("notifiabilityLevel", NotifiabilityLevel.class);
		}

		
	}

	
}
