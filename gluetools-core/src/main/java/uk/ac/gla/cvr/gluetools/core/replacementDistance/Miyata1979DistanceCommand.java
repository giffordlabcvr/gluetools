package uk.ac.gla.cvr.gluetools.core.replacementDistance;

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
import uk.ac.gla.cvr.gluetools.core.translation.MiyataDistanceCalculator;

@CommandClass(
		commandWords={"distance"}, 
		description = "Calculate replacement distance using the method of Miyata 1979", 
		docoptUsages = {"<originalAA> <replacementAA>"},
		furtherHelp = "",
		metaTags = {}	
)
public class Miyata1979DistanceCommand extends ModulePluginCommand<Miyata1979DistanceResult, Miyata1979DistanceCalculator>{

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
			throw new CommandException(Code.COMMAND_USAGE_ERROR, "Distance may not be calculated for stop codons");
		}
		if(originalAA.equals("X") || replacementAA.equals("X")) {
			throw new CommandException(Code.COMMAND_USAGE_ERROR, "Distance may not be calculated for unknown amino acids (X)");
		}
	}

	@Override
	protected Miyata1979DistanceResult execute(CommandContext cmdContext, Miyata1979DistanceCalculator miyata1979DistanceCalculator) {
		return new Miyata1979DistanceResult(MiyataDistanceCalculator.miyataDistance(originalAA.charAt(0), replacementAA.charAt(0)));
	}

	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
		}
		
	}
	
}
