package uk.ac.gla.cvr.gluetools.core.collation.importing.fasta.alignment;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.cayenne.query.SelectQuery;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.collation.importing.fasta.alignment.BaseFastaAlignmentImporter.FastaAlignmentImporterResult;
import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.CompletionSuggestion;
import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter.VariableInstantiator;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModulePluginCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ProvidedProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.source.Source;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@CommandClass( 
		commandWords={"import"}, 
		docoptUsages={"<alignmentName> -f <fileName> [-s <sourceName>] [-n <navAlmtName>]"},
		docCategory = "Type-specific module commands",
		docoptOptions={
				"-f <fileName>, --fileName <fileName>           FASTA file",
				"-s <sourceName>, --sourceName <sourceName>     Restrict alignment members to a given source",
				"-n <navAlmtName>, --navAlmtName <navAlmtName>  Use constrained alignment to navigate"},
		description="Import an unconstrained alignment from a FASTA file", 
		metaTags = { CmdMeta.consoleOnly, CmdMeta.updatesDatabase },
		furtherHelp="The file is loaded from a location relative to the current load/save directory. "+
		"An existing unconstrained alignment will be updated with new members, or a new unconstrained alignment will be created.") 
public class BlastFastaAlignmentImporterImportCommand extends ModulePluginCommand<FastaAlignmentImporterResult, BlastFastaAlignmentImporter> implements ProvidedProjectModeCommand {

	private String fileName;
	private String alignmentName;
	private String sourceName;
	private String navAlignmentName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		fileName = PluginUtils.configureStringProperty(configElem, "fileName", true);
		alignmentName = PluginUtils.configureStringProperty(configElem, "alignmentName", true);
		sourceName = PluginUtils.configureStringProperty(configElem, "sourceName", false);
		navAlignmentName = PluginUtils.configureStringProperty(configElem, "navAlmtName", false);
	}
	
	@Override
	protected FastaAlignmentImporterResult execute(CommandContext cmdContext, BlastFastaAlignmentImporter importerPlugin) {
		Alignment navAlignment = null;
		if(navAlignmentName != null) {
			navAlignment = GlueDataObject.lookup(cmdContext, Alignment.class, Alignment.pkMap(navAlignmentName));
		}
		Alignment alignment = importerPlugin.initAlignment(cmdContext, alignmentName);
		return importerPlugin.doImport((ConsoleCommandContext) cmdContext, fileName, alignment, sourceName, navAlignment);
	}
	
	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerVariableInstantiator("alignmentName", new VariableInstantiator() {
				@SuppressWarnings("rawtypes")
				@Override
				protected List<CompletionSuggestion> instantiate(
						ConsoleCommandContext cmdContext,
						Class<? extends Command> cmdClass, Map<String, Object> bindings,
						String prefix) {
					return GlueDataObject.query(cmdContext, Alignment.class, new SelectQuery(Alignment.class))
							.stream()
							.filter(almt -> !almt.isConstrained())
							.map(almt -> new CompletionSuggestion(almt.getName(), true))
							.collect(Collectors.toList());
				}
			});
			registerDataObjectNameLookup("sourceName", Source.class, Source.NAME_PROPERTY);
			registerPathLookup("fileName", false);
			registerVariableInstantiator("navAlmtName", new VariableInstantiator() {
				@SuppressWarnings("rawtypes")
				@Override
				protected List<CompletionSuggestion> instantiate(
						ConsoleCommandContext cmdContext,
						Class<? extends Command> cmdClass, Map<String, Object> bindings,
						String prefix) {
					return GlueDataObject.query(cmdContext, Alignment.class, new SelectQuery(Alignment.class))
							.stream()
							.filter(almt -> almt.isConstrained())
							.map(almt -> new CompletionSuggestion(almt.getName(), true))
							.collect(Collectors.toList());
				}
			});
		}
	}

}
