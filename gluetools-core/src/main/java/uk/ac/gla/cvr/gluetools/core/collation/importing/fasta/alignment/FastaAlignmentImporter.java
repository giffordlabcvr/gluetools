package uk.ac.gla.cvr.gluetools.core.collation.importing.fasta.alignment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionException;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.SelectQuery;
import org.biojava.nbio.core.sequence.DNASequence;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.collation.importing.fasta.alignment.FastaAlignmentImporterException.Code;
import uk.ac.gla.cvr.gluetools.core.collation.populating.regex.RegexExtractorFormatter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModuleProvidedCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ProvidedProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ShowConfigCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.SimpleConfigureCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.SimpleConfigureCommandClass;
import uk.ac.gla.cvr.gluetools.core.command.result.TableResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignedSegment.AlignedSegment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;
import freemarker.core.ParseException;
import freemarker.template.Template;

@PluginClass(elemName="fastaAlignmentImporter")
public class FastaAlignmentImporter extends ModulePlugin<FastaAlignmentImporter> {

	public static final String IGNORE_REGEX_MATCH_FAILURES = "ignoreRegexMatchFailures";
	public static final String IGNORE_MISSING_SEQUENCES = "ignoreMissingSequences";
	public static final String SEQUENCE_GAP_REGEX = "sequenceGapRegex";
	public static final String REQUIRE_TOTAL_COVERAGE = "requireTotalCoverage";
	public static final String ALLOW_AMBIGUOUS_SEGMENTS = "allowAmbiguousSegments";
	public static final String UPDATE_EXISTING_MEMBERS = "updateExistingMembers";
	public static final String UPDATE_EXISTING_ALIGNMENT = "updateExistingAlignment";
	public static final String ID_CLAUSE_EXTRACTOR_FORMATTER = "idClauseExtractorFormatter";
	
	private RegexExtractorFormatter idClauseExtractorFormatter = null;
	
	private Boolean ignoreRegexMatchFailures = false;
	private Boolean ignoreMissingSequences = false;
	private Boolean requireTotalCoverage = true;
	private Pattern sequenceGapRegex = null;
	private Boolean updateExistingMembers = false;
	private Boolean updateExistingAlignment = false;
	private Boolean allowAmbiguousSegments = false;
	
	public FastaAlignmentImporter() {
		super();
		addProvidedCmdClass(ShowImporterCommand.class);
		addProvidedCmdClass(ImportCommand.class);
		addProvidedCmdClass(ConfigureImporterCommand.class);
	}

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		ignoreRegexMatchFailures = Optional
				.ofNullable(PluginUtils.configureBooleanProperty(configElem, IGNORE_REGEX_MATCH_FAILURES, false))
				.orElse(false);
		ignoreMissingSequences = Optional
				.ofNullable(PluginUtils.configureBooleanProperty(configElem, IGNORE_MISSING_SEQUENCES, false))
				.orElse(false);
		sequenceGapRegex = Optional
				.ofNullable(PluginUtils.configureRegexPatternProperty(configElem, SEQUENCE_GAP_REGEX, false))
				.orElse(Pattern.compile("[Nn-]"));
		requireTotalCoverage = Optional
				.ofNullable(PluginUtils.configureBooleanProperty(configElem, REQUIRE_TOTAL_COVERAGE, false))
				.orElse(true);
		updateExistingMembers = Optional
				.ofNullable(PluginUtils.configureBooleanProperty(configElem, UPDATE_EXISTING_MEMBERS, false))
				.orElse(false);
		updateExistingAlignment = Optional
				.ofNullable(PluginUtils.configureBooleanProperty(configElem, UPDATE_EXISTING_ALIGNMENT, false))
				.orElse(false);
		allowAmbiguousSegments = Optional
				.ofNullable(PluginUtils.configureBooleanProperty(configElem, ALLOW_AMBIGUOUS_SEGMENTS, false))
				.orElse(false);
		
		Element extractorElem = PluginUtils.findConfigElement(configElem, ID_CLAUSE_EXTRACTOR_FORMATTER);
		if(extractorElem != null) {
			idClauseExtractorFormatter = PluginFactory.createPlugin(pluginConfigContext, RegexExtractorFormatter.class, extractorElem);
		} else {
			idClauseExtractorFormatter = new RegexExtractorFormatter();
		}
		if(idClauseExtractorFormatter.getMatchPattern() == null) {
			idClauseExtractorFormatter.setMatchPattern(Pattern.compile("(.*)"));
		}
		if(idClauseExtractorFormatter.getOutputTemplate() == null) {
			Template defaultTemplate;
			try {
				defaultTemplate = PluginUtils.templateFromString("sequenceID = '${g1}'", pluginConfigContext.getFreemarkerConfiguration());
			} catch (ParseException e) {
				throw new RuntimeException(e);
			}
			idClauseExtractorFormatter.setOutputTemplate(defaultTemplate);
		}
		
	}

	public FastaAlignmentImporterResult doImport(ConsoleCommandContext cmdContext, String fileName, String alignmentName, String sourceName) {
		byte[] fastaFileBytes = cmdContext.loadBytes(fileName);
		ObjectContext objContext = cmdContext.getObjectContext();
		Alignment alignment = GlueDataObject.create(objContext, Alignment.class, Alignment.pkMap(alignmentName), updateExistingAlignment);

		ReferenceSequence refSequence = alignment.getRefSequence();
		if(refSequence != null) {
			throw new FastaAlignmentImporterException(Code.ALIGNMENT_IS_CONSTRAINED, alignmentName, refSequence.getName());
		}
		
		Map<String, DNASequence> sequenceMap = FastaUtils.parseFasta(fastaFileBytes);
		
		List<Map<String, Object>> resultListOfMaps = new ArrayList<Map<String, Object>>();
		
		for(Map.Entry<String, DNASequence> entry: sequenceMap.entrySet()) {
			String fastaID = entry.getKey();
			DNASequence dnaSequence = entry.getValue();
			String whereClauseString = idClauseExtractorFormatter.matchAndConvert(fastaID);
			if(whereClauseString == null) {
				if(ignoreRegexMatchFailures) {
					continue;
				}
				throw new FastaAlignmentImporterException(Code.NO_FASTA_ID_REGEX_MATCH, fastaID);
			}
			Expression whereClauseExp = null;
			try {
				whereClauseExp = Expression.fromString(whereClauseString);
			} catch(ExpressionException ee) {
				throw new FastaAlignmentImporterException(Code.INVALID_WHERE_CLAUSE, fastaID, whereClauseString);
			}
			if(sourceName != null) {
				whereClauseExp = ExpressionFactory
						.matchExp(Sequence.SOURCE_NAME_PATH, sourceName)
						.andExp(whereClauseExp);
			}
			List<Sequence> foundSequences = GlueDataObject.query(objContext, Sequence.class, new SelectQuery(Sequence.class, whereClauseExp));
			if(foundSequences.isEmpty()) {
				if(ignoreMissingSequences) {
					continue;
				}
				throw new FastaAlignmentImporterException(Code.NO_SEQUENCE_FOUND, fastaID, whereClauseString);
			}
			if(foundSequences.size() > 1) {
				throw new FastaAlignmentImporterException(Code.MULTIPLE_SEQUENCES_FOUND, fastaID, whereClauseString);
			}
			Sequence foundSequence = foundSequences.get(0);
			String memberSourceName = foundSequence.getSource().getName();
			String memberSequenceID = foundSequence.getSequenceID();
			AlignmentMember almtMember = 
					GlueDataObject.create(objContext, AlignmentMember.class, 
					AlignmentMember.pkMap(alignmentName, memberSourceName, memberSequenceID), updateExistingMembers);
			almtMember.setAlignment(alignment);
			almtMember.setSequence(foundSequence);
			
			List<QueryAlignedSegment> existingSegs = almtMember.getAlignedSegments().stream()
					.map(AlignedSegment::asQueryAlignedSegment)
					.collect(Collectors.toList());
			
			List<QueryAlignedSegment> queryAlignedSegs = findAlignedSegs(cmdContext, foundSequence, existingSegs, dnaSequence.getSequenceAsString(), 
					fastaID, whereClauseString);
			for(QueryAlignedSegment queryAlignedSeg: queryAlignedSegs) {
				AlignedSegment alignedSegment = GlueDataObject.create(objContext, AlignedSegment.class, 
						AlignedSegment.pkMap(alignmentName, memberSourceName, memberSequenceID, 
								queryAlignedSeg.getRefStart(), queryAlignedSeg.getRefEnd(), 
								queryAlignedSeg.getQueryStart(), queryAlignedSeg.getQueryEnd()), false);
				alignedSegment.setAlignmentMember(almtMember);
			}
			Map<String, Object> memberResultMap = new LinkedHashMap<String, Object>();
			memberResultMap.put("fastaID", fastaID);
			memberResultMap.put("sourceName", memberSourceName);
			memberResultMap.put("sequenceID", memberSequenceID);
			memberResultMap.put("numSegmentsAdded", new Integer(queryAlignedSegs.size()));
			resultListOfMaps.add(memberResultMap);
		}
		
		cmdContext.commit();
		return new FastaAlignmentImporterResult(resultListOfMaps);
	}

	private static class FastaAlignmentImporterResult extends TableResult {
		public FastaAlignmentImporterResult(List<Map<String, Object>> rowData) {
			super("fastaAlignmentImporterResult", Arrays.asList("fastaID", "sourceName", "sequenceID", "numSegmentsAdded"), rowData);
		}
	}
	
	private List<QueryAlignedSegment> findAlignedSegs(CommandContext cmdContext, Sequence foundSequence, 
			List<QueryAlignedSegment> existingSegs, String fastaAlignmentNTs, 
			String fastaID, String whereClauseString) {

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
							fastaAlignmentNTs, fastaID, whereClauseString,
							foundSequenceNTs, queryAlignedSegs,
							foundSequenceNtIndex, queryAlignedSeg);
    				queryAlignedSeg = null;
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
					fastaAlignmentNTs, fastaID, whereClauseString,
					foundSequenceNTs, queryAlignedSegs,
					foundSequenceNtIndex, queryAlignedSeg);
		}
		if(requireTotalCoverage && foundSequenceNtIndex != foundSequenceNTs.length()+1) {
			throw new FastaAlignmentImporterException(Code.MISSING_COVERAGE, foundSequenceNtIndex, foundSequenceNTs.length(), fastaID, whereClauseString);
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
			throw new FastaAlignmentImporterException(Code.SUBSEQUENCE_NOT_FOUND, refStart, refEnd, fastaID, whereClauseString);
		}
		if(requireTotalCoverage && foundIdxOfSubseq != foundSequenceNtIndex) {
			throw new FastaAlignmentImporterException(Code.MISSING_COVERAGE, foundSequenceNtIndex, foundIdxOfSubseq-1, fastaID, whereClauseString);
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
	
	private boolean isGapChar(char seqChar) {
		return sequenceGapRegex.matcher(new String(new char[]{seqChar})).find();
	}
	
	@CommandClass( 
			commandWords={"import"}, 
			docoptUsages={"<alignmentName> -f <file> [-s <sourceName>]"},
			docoptOptions={
			"-f <file>, --fileName <file>  FASTA file",
			"-s <sourceName>, --sourceName <sourceName>  Restrict alignment members to a given source"},
			description="Import an unconstrained alignment from a FASTA file", 
			metaTags = { CmdMeta.consoleOnly, CmdMeta.updatesDatabase },
			furtherHelp="The file is loaded from a location relative to the current load/save directory. "+
			"An existing unconstrained alignment will be updated with new members, or a new unconstrained alignment will be created.") 
	public static class ImportCommand extends ModuleProvidedCommand<FastaAlignmentImporterResult, FastaAlignmentImporter> implements ProvidedProjectModeCommand {

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
			return importerPlugin.doImport((ConsoleCommandContext) cmdContext, fileName, alignmentName, sourceName);
		}
	}
	
	@CommandClass( 
			commandWords={"show", "configuration"}, 
			docoptUsages={},
			description="Show the current configuration of this importer") 
	public static class ShowImporterCommand extends ShowConfigCommand<FastaAlignmentImporter> {}

	
	@SimpleConfigureCommandClass(
			propertyNames={IGNORE_REGEX_MATCH_FAILURES, IGNORE_MISSING_SEQUENCES, SEQUENCE_GAP_REGEX, 
					REQUIRE_TOTAL_COVERAGE, UPDATE_EXISTING_MEMBERS, UPDATE_EXISTING_ALIGNMENT}
	)
	public static class ConfigureImporterCommand extends SimpleConfigureCommand<FastaAlignmentImporter> {}
	
}
