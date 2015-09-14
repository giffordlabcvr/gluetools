package uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.featureLoc.variation;

import java.util.List;
import java.util.Optional;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.CompleterUtils;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.OkResult;
import uk.ac.gla.cvr.gluetools.core.command.result.UpdateResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.Variation;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.Variation.NotifiabilityLevel;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@CommandClass( 
		commandWords={"set","notifiability"}, 
		docoptUsages={"<notifiabilityLevel>"},
		docoptOptions={},
		metaTags={CmdMeta.updatesDatabase},
		description="Set the variation's notifiability level", 
		furtherHelp="Possible values for the notifiability level are [ACTIONABLE, NOT_ACTIONABLE]") 
public class VariationSetNotifiabilityCommand extends VariationModeCommand<OkResult> {

	public static final String NOTIFIABILITY_LEVEL = "notifiabilityLevel";
	
	private NotifiabilityLevel notifiabilityLevel;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		notifiabilityLevel = Optional.ofNullable(
				PluginUtils.configureEnumProperty(NotifiabilityLevel.class, configElem, NOTIFIABILITY_LEVEL, false)).
				orElse(NotifiabilityLevel.ACTIONABLE);
	}

	@Override
	public OkResult execute(CommandContext cmdContext) {
		Variation variation = lookupVariation(cmdContext);
		variation.setNotifiability(notifiabilityLevel.name());
		cmdContext.commit();
		return new UpdateResult(Variation.class, 1);
	}

	@CompleterClass
	public static class MetatagTypeCompleter extends CommandCompleter {

		@SuppressWarnings("rawtypes")
		@Override
		public List<String> completionSuggestions(
				ConsoleCommandContext cmdContext,
				Class<? extends Command> cmdClass, List<String> argStrings) {
			return CompleterUtils.enumCompletionSuggestions(NotifiabilityLevel.class);
		}
		
	}

	
}
