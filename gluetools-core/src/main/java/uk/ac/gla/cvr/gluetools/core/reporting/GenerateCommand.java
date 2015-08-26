package uk.ac.gla.cvr.gluetools.core.reporting;

import java.util.Optional;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModuleProvidedCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ProvidedProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.reporting.MutationFrequenciesReporter.MutationFrequenciesResult;

@CommandClass(
		commandWords="generate", 
		description = "Analyse mutations for a given taxon/feature", 
		docoptUsages = { "[-t <taxon>] -f <feature>" }, 
		docoptOptions = {
				"-t <taxon>, --taxon <taxon>        Restrict analysis by taxon",
				"-f <feature>, --feature <feature>  Specify genome feature"
		}
)
public class GenerateCommand extends ModuleProvidedCommand<MutationFrequenciesResult, MutationFrequenciesReporter> implements ProvidedProjectModeCommand {
	
	private Optional<String> taxon;
	private String feature;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		taxon = Optional.ofNullable(PluginUtils.configureStringProperty(configElem, "taxon", false));
		feature = PluginUtils.configureStringProperty(configElem, "feature", true);
	}

	@Override
	protected MutationFrequenciesResult execute(CommandContext cmdContext, MutationFrequenciesReporter mutationFrequenciesPlugin) {
		return mutationFrequenciesPlugin.doGenerate(cmdContext, taxon, feature);
	}

}