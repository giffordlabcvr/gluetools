/**
 *    GLUE: A flexible system for virus sequence data
 *    Copyright (C) 2018 The University of Glasgow
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Affero General Public License as published
 *    by the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Affero General Public License for more details.

 *    You should have received a copy of the GNU Affero General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *    Contact details:
 *    MRC-University of Glasgow Centre for Virus Research
 *    Sir Michael Stoker Building, Garscube Campus, 464 Bearsden Road, 
 *    Glasgow G61 1QH, United Kingdom
 *    
 *    Josh Singer: josh.singer@glasgow.ac.uk
 *    Rob Gifford: robert.gifford@glasgow.ac.uk
*/
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
		docoptOptions={
				"-f <fileName>, --fileName <fileName>           FASTA file",
				"-s <sourceName>, --sourceName <sourceName>     Restrict alignment members to a given source",
				"-n <navAlmtName>, --navAlmtName <navAlmtName>  Use constrained alignment to navigate"},
		description="Import an unconstrained alignment from a FASTA file", 
		metaTags = { CmdMeta.consoleOnly},
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
				public List<CompletionSuggestion> instantiate(
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
				public List<CompletionSuggestion> instantiate(
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
