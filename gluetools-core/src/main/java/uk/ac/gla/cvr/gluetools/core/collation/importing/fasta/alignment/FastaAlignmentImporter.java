package uk.ac.gla.cvr.gluetools.core.collation.importing.fasta.alignment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.cayenne.query.SelectQuery;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.collation.importing.fasta.alignment.FastaAlignmentImporterException.Code;
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
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.source.Source;
import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;

@PluginClass(elemName="fastaAlignmentImporter")
public class FastaAlignmentImporter extends FastaNtAlignmentImporter<FastaAlignmentImporter> {

	public static final String SEQUENCE_GAP_REGEX = "sequenceGapRegex";
	public static final String REQUIRE_TOTAL_COVERAGE = "requireTotalCoverage";
	public static final String ALLOW_AMBIGUOUS_SEGMENTS = "allowAmbiguousSegments";
	public static final String SKIP_ROWS_WITH_MISSING_SEGMENTS = "skipRowsWithMissingSegments";

	
	private Boolean requireTotalCoverage = true;
	private Pattern sequenceGapRegex = null;
	private Boolean allowAmbiguousSegments = false;
	private Boolean skipRowsWithMissingSegments;

	public FastaAlignmentImporter() {
		super();
		addModulePluginCmdClass(ImportCommand.class);
		addModulePluginCmdClass(PreviewCommand.class);
		addSimplePropertyName(SEQUENCE_GAP_REGEX);
		addSimplePropertyName(REQUIRE_TOTAL_COVERAGE);
		addSimplePropertyName(ALLOW_AMBIGUOUS_SEGMENTS);
		addSimplePropertyName(SKIP_ROWS_WITH_MISSING_SEGMENTS);
	}

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		sequenceGapRegex = Optional
				.ofNullable(PluginUtils.configureRegexPatternProperty(configElem, SEQUENCE_GAP_REGEX, false))
				.orElse(Pattern.compile("[Nn-]"));
		requireTotalCoverage = Optional
				.ofNullable(PluginUtils.configureBooleanProperty(configElem, REQUIRE_TOTAL_COVERAGE, false))
				.orElse(true);
		allowAmbiguousSegments = Optional
				.ofNullable(PluginUtils.configureBooleanProperty(configElem, ALLOW_AMBIGUOUS_SEGMENTS, false))
				.orElse(false);
		skipRowsWithMissingSegments = Optional
				.ofNullable(PluginUtils.configureBooleanProperty(configElem, SKIP_ROWS_WITH_MISSING_SEGMENTS, false))
				.orElse(false);

		
	}

	


	@Override
	protected List<QueryAlignedSegment> findAlignedSegs(CommandContext cmdContext, Sequence foundSequence, 
			List<QueryAlignedSegment> existingSegs, String fastaAlignmentNTs, 
			String fastaID) {

		String foundSequenceNTs = foundSequence.getSequenceObject().getNucleotides(cmdContext);
		
		List<QueryAlignedSegment> queryAlignedSegs = new ArrayList<QueryAlignedSegment>();
    	
    	int alignmentNtIndex = 1;
    	int foundSequenceNtIndex = 1;

    	QueryAlignedSegment queryAlignedSeg = null;
    	while(alignmentNtIndex <= fastaAlignmentNTs.length()) {
    		char fastaAlignmentNT = FastaUtils.nt(fastaAlignmentNTs, alignmentNtIndex);
    		if(isGapChar(fastaAlignmentNT)) {
    			if(queryAlignedSeg != null) {
    				foundSequenceNtIndex = completeQueryAlignedSeg(existingSegs,
    						fastaAlignmentNTs, fastaID, foundSequence.pkMap().toString(),
    						foundSequenceNTs, queryAlignedSegs,
    						foundSequenceNtIndex, queryAlignedSeg);
    				queryAlignedSeg = null;
    				if(foundSequenceNtIndex == -1) {
    					return null;
    				}        					
    			}
    		} else {
    			if(queryAlignedSeg == null) {
    				queryAlignedSeg = new QueryAlignedSegment(alignmentNtIndex, alignmentNtIndex, 1, 1);
    			}
    			queryAlignedSeg.setRefEnd(alignmentNtIndex);
    		}
			alignmentNtIndex++;
    	}
		if(queryAlignedSeg != null) {
			foundSequenceNtIndex = completeQueryAlignedSeg(existingSegs,
					fastaAlignmentNTs, fastaID, foundSequence.pkMap().toString(),
					foundSequenceNTs, queryAlignedSegs,
					foundSequenceNtIndex, queryAlignedSeg);
		}
		if(requireTotalCoverage && foundSequenceNtIndex != foundSequenceNTs.length()+1) {
			throw new FastaAlignmentImporterException(Code.MISSING_COVERAGE, foundSequenceNtIndex, foundSequenceNTs.length(), fastaID, foundSequence.pkMap().toString());
		}
		return queryAlignedSegs;
	}

	public int completeQueryAlignedSeg(List<QueryAlignedSegment> existingSegs, String fastaAlignmentNTs,
			String fastaID, String whereClauseString, String foundSequenceNTs,
			List<QueryAlignedSegment> queryAlignedSegs,
			int foundSequenceNtIndex, QueryAlignedSegment queryAlignedSeg) {
		Integer refStart = queryAlignedSeg.getRefStart();
		Integer refEnd = queryAlignedSeg.getRefEnd();
		String subSequence = FastaUtils
				.subSequence(fastaAlignmentNTs, refStart, refEnd).toString().toUpperCase();
		int foundIdxOfSubseq = FastaUtils.find(foundSequenceNTs, subSequence, foundSequenceNtIndex);
		if(foundIdxOfSubseq == -1) {
			if(skipRowsWithMissingSegments) {
				logMissingSegmentSkippedRow(fastaID, refStart, refEnd, whereClauseString);
				return -1;
			} else {
				throw new FastaAlignmentImporterException(Code.SUBSEQUENCE_NOT_FOUND, refStart, refEnd, fastaID, whereClauseString);
			}
		}
		if(requireTotalCoverage && foundIdxOfSubseq != foundSequenceNtIndex) {
			throw new FastaAlignmentImporterException(Code.MISSING_COVERAGE, foundSequenceNtIndex, foundIdxOfSubseq-1, fastaID);
		}
		if(!allowAmbiguousSegments) {
			int nextIdxOfSubseq = FastaUtils.find(foundSequenceNTs, subSequence, foundIdxOfSubseq+1);
			if(nextIdxOfSubseq != -1) {
				throw new FastaAlignmentImporterException(Code.AMBIGUOUS_SEGMENT, refStart, refEnd, fastaID, whereClauseString, foundSequenceNtIndex);
			}
		}
		queryAlignedSeg.setQueryStart(foundIdxOfSubseq);
		queryAlignedSeg.setQueryEnd(foundIdxOfSubseq+(subSequence.length()-1));

		List<QueryAlignedSegment> intersection = ReferenceSegment.intersection(existingSegs, 
				Collections.singletonList(queryAlignedSeg), 
				ReferenceSegment.cloneLeftSegMerger());
		if(!intersection.isEmpty()) {
			QueryAlignedSegment firstOverlap = intersection.get(0);
			throw new FastaAlignmentImporterException(Code.SEGMENT_OVERLAPS_EXISTING, 
					firstOverlap.getRefStart(), firstOverlap.getRefEnd(), 
					fastaID, whereClauseString);
		}
		queryAlignedSegs.add(queryAlignedSeg);
		existingSegs.add(queryAlignedSeg);
		foundSequenceNtIndex = queryAlignedSeg.getQueryEnd()+1;
		return foundSequenceNtIndex;
	}
	
	private void logMissingSegmentSkippedRow(String fastaId, int startColumnNumber, int endColumnNumber, String whereClause) {
		GlueLogger.getGlueLogger().warning("Skipping alignment row "+fastaId+
				": segment ["+startColumnNumber+", "+endColumnNumber+
				"] is missing in sequence identified by "+whereClause);
	}

	
	private boolean isGapChar(char seqChar) {
		return sequenceGapRegex.matcher(new String(new char[]{seqChar})).find();
	}
	
	@CommandClass( 
			commandWords={"import"}, 
			docoptUsages={"<alignmentName> -f <fileName> [-s <sourceName>]"},
			docoptOptions={
			"-f <fileName>, --fileName <fileName>        FASTA file",
			"-s <sourceName>, --sourceName <sourceName>  Restrict alignment members to a given source"},
			description="Import an unconstrained alignment from a FASTA file", 
			metaTags = { CmdMeta.consoleOnly, CmdMeta.updatesDatabase },
			furtherHelp="The file is loaded from a location relative to the current load/save directory. "+
			"An existing unconstrained alignment will be updated with new members, or a new unconstrained alignment will be created.") 
	public static class ImportCommand extends ModulePluginCommand<FastaAlignmentImporterResult, FastaAlignmentImporter> implements ProvidedProjectModeCommand {

		private String fileName;
		private String alignmentName;
		private String sourceName;
		
		@Override
		public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
			super.configure(pluginConfigContext, configElem);
			fileName = PluginUtils.configureStringProperty(configElem, "fileName", true);
			alignmentName = PluginUtils.configureStringProperty(configElem, "alignmentName", true);
			sourceName = PluginUtils.configureStringProperty(configElem, "sourceName", false);
		}
		
		@Override
		protected FastaAlignmentImporterResult execute(CommandContext cmdContext, FastaAlignmentImporter importerPlugin) {
			Alignment alignment = importerPlugin.initAlignment(cmdContext, alignmentName);
			return importerPlugin.doImport((ConsoleCommandContext) cmdContext, fileName, alignment, sourceName);
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
				registerDataObjectNameLookup("alignmentName", Alignment.class, Alignment.NAME_PROPERTY);
				registerDataObjectNameLookup("sourceName", Source.class, Source.NAME_PROPERTY);
				registerPathLookup("fileName", false);
			}
		}

	}
	
	@CommandClass( 
			commandWords={"preview"}, 
			docoptUsages={"-f <fileName> [-s <sourceName>]"},
			docoptOptions={
			"-f <fileName>, --fileName <fileName>        FASTA file",
			"-s <sourceName>, --sourceName <sourceName>  Restrict alignment members to a given source"},
			description="Preview import of an unconstrained alignment from a FASTA file", 
			metaTags = { CmdMeta.consoleOnly, CmdMeta.updatesDatabase },
			furtherHelp="The file is loaded from a location relative to the current load/save directory. ") 
	public static class PreviewCommand extends ModulePluginCommand<FastaAlignmentImporterResult, FastaAlignmentImporter> implements ProvidedProjectModeCommand {

		private String fileName;
		private String sourceName;
		
		@Override
		public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
			super.configure(pluginConfigContext, configElem);
			fileName = PluginUtils.configureStringProperty(configElem, "fileName", true);
			sourceName = PluginUtils.configureStringProperty(configElem, "sourceName", false);
		}
		
		@Override
		protected FastaAlignmentImporterResult execute(CommandContext cmdContext, FastaAlignmentImporter importerPlugin) {
			return importerPlugin.doPreview((ConsoleCommandContext) cmdContext, fileName, sourceName);
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
			}
		}

	}

	
}
