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
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.codonNumbering.LabeledCodonReferenceSegment;
import uk.ac.gla.cvr.gluetools.core.codonNumbering.LabeledQueryAminoAcid;
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
import uk.ac.gla.cvr.gluetools.core.command.project.alignment.AlignmentBaseListMemberCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModulePluginCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ProvidedProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.AminoAcidFastaCommandResult;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.result.OkResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;
import uk.ac.gla.cvr.gluetools.core.translation.CommandContextTranslator;
import uk.ac.gla.cvr.gluetools.core.translation.Translator;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils.LineFeedStyle;
import uk.ac.gla.cvr.gluetools.utils.fasta.ProteinSequence;

@CommandClass( 
		commandWords={"export", "member"}, 
		docoptUsages={"<alignmentName> -r <relRefName> -f <featureName> (-w <whereClause> | -a) [-y <lineFeedStyle>] (-p | -o <fileName>)"},
		docoptOptions={
				"-r <relRefName>, --relRefName <relRefName>           Related reference",
				"-f <featureName>, --featureName <featureName>        Protein-coding feature",
				"-y <lineFeedStyle>, --lineFeedStyle <lineFeedStyle>  LF or CRLF",
				"-o <fileName>, --fileName <fileName>                 Output FASTA file",
				"-w <whereClause>, --whereClause <whereClause>        Qualify exported members",
			    "-a, --allMembers                                     Export all alignment members",
				"-p, --preview                                        Preview output"},
		metaTags = { CmdMeta.consoleOnly },
		description="Export alignment member coding feature(s) to a FASTA file", 
		furtherHelp="The file is saved to a location relative to the current load/save directory.") 
public class ExportMemberCommand extends ModulePluginCommand<CommandResult, FastaProteinExporter> implements ProvidedProjectModeCommand {

	public static final String ALIGNMENT_NAME = "alignmentName";
	public static final String REL_REF_NAME = "relRefName";
	public static final String FEATURE_NAME = "featureName";
	public static final String WHERE_CLAUSE = "whereClause";
	public static final String ALL_MEMBERS = "allMembers";
	public static final String LINE_FEED_STYLE = "lineFeedStyle";
	public static final String PREVIEW = "preview";
	public static final String FILE_NAME = "fileName";


	private String alignmentName;
	private String relRefName;
	private String featureName;
	private Expression whereClause;
	private Boolean allMembers;
	private LineFeedStyle lineFeedStyle;
	private Boolean preview;
	private String fileName;

	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		alignmentName = PluginUtils.configureStringProperty(configElem, ALIGNMENT_NAME, true);
		relRefName = PluginUtils.configureStringProperty(configElem, REL_REF_NAME, true);
		featureName = PluginUtils.configureStringProperty(configElem, FEATURE_NAME, true);
		whereClause = PluginUtils.configureCayenneExpressionProperty(configElem, WHERE_CLAUSE, false);
		allMembers = PluginUtils.configureBooleanProperty(configElem, ALL_MEMBERS, false);
		if(whereClause == null && !allMembers) {
			usageError();
		}
		if(whereClause != null && allMembers) {
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
		throw new CommandException(Code.COMMAND_USAGE_ERROR, "Either <whereClause> or <allMembers> must be specified, but not both");
	}

	
	@Override
	protected CommandResult execute(CommandContext cmdContext, FastaProteinExporter fastaProteinExporter) {
		Alignment alignment = GlueDataObject.lookup(cmdContext, Alignment.class, Alignment.pkMap(alignmentName));
		ReferenceSequence relRefSeq = alignment.getRelatedRef(cmdContext, relRefName);
		FeatureLocation featureLoc = GlueDataObject.lookup(cmdContext, FeatureLocation.class, FeatureLocation.pkMap(relRefName, featureName));
		featureLoc.getFeature().checkCodesAminoAcids();
		
		Expression memberExp = AlignmentBaseListMemberCommand.getMatchExpression(alignment, false, Optional.ofNullable(whereClause));
		SelectQuery memberSelect = new SelectQuery(AlignmentMember.class, memberExp);
		List<AlignmentMember> almtMembers = GlueDataObject.query(cmdContext, AlignmentMember.class, memberSelect);
		Map<String, ProteinSequence> aaFastaMap = aaFastaMap(cmdContext, relRefSeq, featureLoc, alignment, almtMembers, fastaProteinExporter);

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
	
	private Map<String, ProteinSequence> aaFastaMap(CommandContext cmdContext,
			ReferenceSequence relRefSeq, FeatureLocation featureLoc,
			Alignment alignment, List<AlignmentMember> almtMembers,
			FastaProteinExporter fastaProteinExporter) {
		
		Translator translator = new CommandContextTranslator(cmdContext);
		Map<String, ProteinSequence> idToProtSeq = new LinkedHashMap<String, ProteinSequence>();
		List<LabeledCodonReferenceSegment> lcRefSegs = featureLoc.getLabeledCodonReferenceSegments(cmdContext);
		almtMembers.forEach(almtMember -> {
			List<QueryAlignedSegment> memberQaSegs = almtMember.segmentsAsQueryAlignedSegments();
			Alignment tipAlmt = almtMember.getAlignment();
			memberQaSegs = tipAlmt.translateToRelatedRef(cmdContext, memberQaSegs, relRefSeq);
			memberQaSegs = ReferenceSegment.intersection(memberQaSegs, lcRefSegs, ReferenceSegment.cloneLeftSegMerger());
			List<LabeledQueryAminoAcid> lqaas = featureLoc.translateQueryNucleotides(cmdContext, translator, memberQaSegs, almtMember.getSequence().getSequenceObject());
			StringBuffer proteinStringBuf = new StringBuffer();
			lqaas.forEach(lqaa -> proteinStringBuf.append(lqaa.getLabeledAminoAcid().getAminoAcid()));
			ProteinSequence proteinSeq = FastaUtils.proteinStringToSequence(proteinStringBuf.toString());
			String fastaId = fastaProteinExporter.generateMemberFastaId(almtMember);
			idToProtSeq.put(fastaId, proteinSeq);
		});

		return idToProtSeq;
	}

	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerDataObjectNameLookup("alignmentName", Alignment.class, Alignment.NAME_PROPERTY);
			registerVariableInstantiator("relRefName", new VariableInstantiator() {
				@SuppressWarnings("rawtypes")
				@Override
				public List<CompletionSuggestion> instantiate(
						ConsoleCommandContext cmdContext,
						Class<? extends Command> cmdClass, Map<String, Object> bindings,
						String prefix) {
					String alignmentName = (String) bindings.get("alignmentName");
					Alignment alignment = GlueDataObject.lookup(cmdContext, Alignment.class, Alignment.pkMap(alignmentName), true);
					if(alignment != null) {
						return(alignment.getRelatedRefs()
								.stream()
								.map(ref -> new CompletionSuggestion(ref.getName(), true)))
								.collect(Collectors.toList());
					}
					return null;
				}
			});
			registerVariableInstantiator("featureName", new VariableInstantiator() {
				@SuppressWarnings("rawtypes")
				@Override
				public List<CompletionSuggestion> instantiate(
						ConsoleCommandContext cmdContext,
						Class<? extends Command> cmdClass, Map<String, Object> bindings,
						String prefix) {
					String relRefName = (String) bindings.get("relRefName");
					ReferenceSequence relRef = GlueDataObject.lookup(cmdContext, ReferenceSequence.class, ReferenceSequence.pkMap(relRefName), true);
					if(relRef != null) {
						return(relRef.getFeatureLocations()
								.stream()
								.filter(fLoc -> fLoc.getFeature().codesAminoAcids())
								.map(fLoc -> new CompletionSuggestion(fLoc.getFeature().getName(), true)))
								.collect(Collectors.toList());
					}
					return null;
				}
			});
			registerEnumLookup("lineFeedStyle", LineFeedStyle.class);
			registerPathLookup("fileName", false);
		}
	}

}
