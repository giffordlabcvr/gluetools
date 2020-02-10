package uk.ac.gla.cvr.gluetools.core.replacementClassification;

import java.util.Arrays;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CommandException.Code;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModulePluginCommand;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.translation.Hanada2006Classification;

@CommandClass(
		commandWords={"classify", "replacement"}, 
		description = "Classify amino acid replacement using Hanada et al. 2006 classification schemes", 
		docoptUsages = {"<originalAA> <replacementAA>"},
		furtherHelp = "",
		metaTags = {}	
)
public class Hanada2006ClassifyReplacementCommand extends ModulePluginCommand<Hanada2006ClassifyReplacementResult, Hanada2006ReplacementClassifier>{

	private String originalAA;
	private String replacementAA;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.originalAA = PluginUtils.configureStringProperty(configElem, "originalAA", true).toUpperCase();
		this.replacementAA = PluginUtils.configureStringProperty(configElem, "replacementAA", true).toUpperCase();
		if(originalAA.length() != 1 || replacementAA.length() != 1) {
			throw new CommandException(Code.COMMAND_USAGE_ERROR, "Compared amino acids must be strings of length 1");
		}
		if(originalAA.equals("*") || replacementAA.equals("*")) {
			throw new CommandException(Code.COMMAND_USAGE_ERROR, "Stop codons may not be compared using this scheme");
		}
		if(originalAA.equals("X") || replacementAA.equals("X")) {
			throw new CommandException(Code.COMMAND_USAGE_ERROR, "Unknown amino acids (X) may not be compared using this scheme");
		}
	}

	@Override
	protected Hanada2006ClassifyReplacementResult execute(CommandContext cmdContext, Hanada2006ReplacementClassifier hanada2006ReplacementClassifier) {
		return new Hanada2006ClassifyReplacementResult(Arrays.asList(Hanada2006Classification.classifyReplacement(originalAA.charAt(0), replacementAA.charAt(0))));
	}

	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
		}
		
	}
	
}
