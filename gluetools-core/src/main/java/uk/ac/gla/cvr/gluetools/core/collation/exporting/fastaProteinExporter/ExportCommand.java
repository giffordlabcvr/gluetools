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
package uk.ac.gla.cvr.gluetools.core.collation.exporting.fastaProteinExporter;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;




import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.query.SelectQuery;
import org.biojava.nbio.core.sequence.ProteinSequence;
import org.w3c.dom.Element;




import uk.ac.gla.cvr.gluetools.core.codonNumbering.LabeledAminoAcid;
import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CommandException.Code;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.CompletionSuggestion;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModulePluginCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ProvidedProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.featureLoc.FeatureLocAminoAcidCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.AminoAcidFastaCommandResult;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.result.OkResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.feature.Feature;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils.LineFeedStyle;

@CommandClass( 
		commandWords={"export"}, 
		docoptUsages={"-f <featureName> (-w <whereClause> | -a) [-y <lineFeedStyle>] (-p | -o <fileName>)"},
		docoptOptions={
				"-f <featureName>, --featureName <featureName>        Protein-coding feature",
				"-y <lineFeedStyle>, --lineFeedStyle <lineFeedStyle>  LF or CRLF",
				"-o <fileName>, --fileName <fileName>                 Output FASTA file",
				"-w <whereClause>, --whereClause <whereClause>        Qualify exported references",
			    "-a, --allReferences                                  Export all project references",
				"-p, --preview                                        Preview output"},
		metaTags = { CmdMeta.consoleOnly },
		description="Export reference sequence coding feature(s) to a FASTA file", 
		furtherHelp="The file is saved to a location relative to the current load/save directory.") 
public class ExportCommand extends ModulePluginCommand<CommandResult, FastaProteinExporter> implements ProvidedProjectModeCommand {

	public static final String PREVIEW = "preview";
	public static final String FILE_NAME = "fileName";
	public static final String FEATURE_NAME = "featureName";
	public static final String LINE_FEED_STYLE = "lineFeedStyle";


	private String featureName;
	private Expression whereClause;
	private Boolean allReferences;
	private LineFeedStyle lineFeedStyle;
	private Boolean preview;
	private String fileName;

	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		featureName = PluginUtils.configureStringProperty(configElem, FEATURE_NAME, true);
		whereClause = PluginUtils.configureCayenneExpressionProperty(configElem, "whereClause", false);
		allReferences = PluginUtils.configureBooleanProperty(configElem, "allReferences", false);
		if(whereClause == null && !allReferences) {
			usageError();
		}
		if(whereClause != null && allReferences) {
			usageError();
		}
		lineFeedStyle = Optional.ofNullable(PluginUtils.configureEnumProperty(LineFeedStyle.class, configElem, LINE_FEED_STYLE, false)).orElse(LineFeedStyle.LF);
		preview = PluginUtils.configureBooleanProperty(configElem, PREVIEW, true);
		fileName = PluginUtils.configureStringProperty(configElem, FILE_NAME, false);
		if(fileName == null && !preview || fileName != null && preview) {
			throw new CommandException(Code.COMMAND_USAGE_ERROR, "Either <fileName> or <preview> must be specified, but not both");
		}
	}

	private void usageError() {
		throw new CommandException(Code.COMMAND_USAGE_ERROR, "Either <whereClause> or <allReferences> must be specified, but not both");
	}

	
	@Override
	protected CommandResult execute(CommandContext cmdContext, FastaProteinExporter fastaProteinExporter) {
		
		Feature feature = GlueDataObject.lookup(cmdContext, Feature.class, Feature.pkMap(featureName));
		SelectQuery refSelect;
		if(whereClause != null) {
			refSelect = new SelectQuery(ReferenceSequence.class, whereClause);
		} else {
			refSelect = new SelectQuery(ReferenceSequence.class);
		}
		
		List<ReferenceSequence> refSeqs = GlueDataObject.query(cmdContext, ReferenceSequence.class, refSelect);
		
		Map<String, ProteinSequence> aaFastaMap = aaFastaMap(cmdContext, refSeqs, feature, fastaProteinExporter);

		if(preview) {
			return new AminoAcidFastaCommandResult(aaFastaMap);
		} else {
			ConsoleCommandContext consoleCmdContext = (ConsoleCommandContext) cmdContext;
			try(OutputStream outputStream = consoleCmdContext.openFile(fileName)) {
				PrintWriter printWriter = new PrintWriter(new BufferedOutputStream(outputStream, 65536));
				aaFastaMap.forEach((fastaId, protSeq) -> {
					printWriter.append(FastaUtils.seqIdCompoundsPairToFasta(fastaId, protSeq.getSequenceAsString(), lineFeedStyle));
				});
				printWriter.flush();
			} catch (IOException ioe) {
				throw new CommandException(ioe, Code.COMMAND_FAILED_ERROR, "Failed to write protein FASTA file: "+ioe.getMessage());
			}
			return new OkResult();
		}
	}
	
	private Map<String, ProteinSequence> aaFastaMap(CommandContext cmdContext, List<ReferenceSequence> refSeqs, 
			Feature feature, FastaProteinExporter fastaProteinExporter) {
		Map<String, ProteinSequence> idToProtSeq = new LinkedHashMap<String, ProteinSequence>();
		refSeqs.forEach(refSeq -> {
			StringBuffer proteinStringBuf = new StringBuffer();
			FeatureLocation featureLoc = 
					GlueDataObject.lookup(cmdContext, FeatureLocation.class, FeatureLocation.pkMap(refSeq.getName(), feature.getName()), true);
			if(featureLoc != null) {
				List<LabeledAminoAcid> labeledAAs = FeatureLocAminoAcidCommand.featureLocAminoAcids(cmdContext, featureLoc);
				labeledAAs.forEach(laa -> proteinStringBuf.append(laa.getAminoAcid()));
			}
			ProteinSequence proteinSeq = FastaUtils.proteinStringToSequence(proteinStringBuf.toString());
			String fastaId = fastaProteinExporter.generateFastaId(refSeq);
			idToProtSeq.put(fastaId, proteinSeq);
		});
		return idToProtSeq;
	}
	
	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerVariableInstantiator("featureName", new VariableInstantiator() {
				@SuppressWarnings("rawtypes")
				@Override
				public List<CompletionSuggestion> instantiate(
						ConsoleCommandContext cmdContext,
						Class<? extends Command> cmdClass, Map<String, Object> bindings,
						String prefix) {
					List<Feature> features = GlueDataObject.query(cmdContext, Feature.class, new SelectQuery(Feature.class));
					return(features
							.stream()
							.filter(f -> f.codesAminoAcids())
							.map(f -> new CompletionSuggestion(f.getName(), true)))
							.collect(Collectors.toList());
				}
			});
			registerEnumLookup("lineFeedStyle", LineFeedStyle.class);
			registerPathLookup("fileName", false);
		}
	}

}
