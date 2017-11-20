package uk.ac.gla.cvr.gluetools.core.collation.freemarker;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModulePluginCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ProvidedProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@CommandClass( 
		commandWords={"transform"}, 
		docoptUsages={"<inputFile> (-p | -r [-E] [-C] [-O] | -o <outputFile>)"},
		docoptOptions={
				"-p, --preview                           Preview GLUE commands",
				"-r, --run                               Run GLUE commands",
				"-E, --no-cmd-echo                       Suppress command echo",
				"-C, --no-comment-echo                   Suppress comment echo",
			  	"-O, --no-output                         Suppress result output",
				"-o <outputFile>, --output <outputFile>  Output commands to a file",
		},
		description="Read a CSV file and transform to GLUE using a template", 
		metaTags = { CmdMeta.consoleOnly }
) 
public class FreemarkerTextToGlueTransformerTransformCommand extends ModulePluginCommand<CommandResult, FreemarkerTextToGlueTransformer> implements ProvidedProjectModeCommand {

	private String inputFile;
	private String outputFile;
	private boolean preview;
	private boolean run;
	private boolean noCmdEcho;
	private boolean noCommentEcho;
	private boolean noOutput;

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		inputFile = PluginUtils.configureStringProperty(configElem, "inputFile", true);
		outputFile = PluginUtils.configureStringProperty(configElem, "output", false);
		preview = PluginUtils.configureBooleanProperty(configElem, "preview", true);
		noCmdEcho = PluginUtils.configureBooleanProperty(configElem, "no-cmd-echo", true);
		noCommentEcho = PluginUtils.configureBooleanProperty(configElem, "no-comment-echo", true);
		noOutput = PluginUtils.configureBooleanProperty(configElem, "no-output", true);
		run = PluginUtils.configureBooleanProperty(configElem, "run", true);
		if(!( 
				(preview && !run && outputFile == null) ||
				(!preview && run && outputFile == null) ||
				(!preview && !run && outputFile != null)
				)) {
			usageError1();
		}
		if(!run && (noCmdEcho || noOutput)) {
			usageError2();
		}
	}

	private void usageError1() {
		throw new CommandException(CommandException.Code.COMMAND_USAGE_ERROR, "Either --preview, --run or --output must be specified");
	}

	private void usageError2() {
		throw new CommandException(CommandException.Code.COMMAND_USAGE_ERROR, "The --no-echo and --no-output options may only be used with --run");
	}
	
	@Override
	protected CommandResult execute(CommandContext cmdContext, FreemarkerTextToGlueTransformer tranformerPlugin) {
		return tranformerPlugin.transform((ConsoleCommandContext) cmdContext, inputFile, preview, run, noCmdEcho, noCommentEcho, noOutput, outputFile);
	}
	
	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerPathLookup("inputFile", false);
			registerPathLookup("outputFile", false);
		}
	}

	
}