package uk.ac.gla.cvr.gluetools.core.collation.importing.fasta.alignment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionException;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.SelectQuery;
import org.biojava.nbio.core.sequence.DNASequence;
import org.w3c.dom.Element;

import freemarker.core.ParseException;
import freemarker.template.Template;
import uk.ac.gla.cvr.gluetools.core.collation.importing.fasta.alignment.FastaAlignmentImporterException.Code;
import uk.ac.gla.cvr.gluetools.core.collation.populating.regex.RegexExtractorFormatter;
import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
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
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.AbstractSequenceObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.source.Source;
import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.core.plugins.Plugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;

public abstract class BaseFastaAlignmentImporter<I extends BaseFastaAlignmentImporter<I>> extends ModulePlugin<I> {

	public static final String IGNORE_REGEX_MATCH_FAILURES = "ignoreRegexMatchFailures";
	public static final String IGNORE_MISSING_SEQUENCES = "ignoreMissingSequences";
	public static final String UPDATE_EXISTING_MEMBERS = "updateExistingMembers";
	public static final String UPDATE_EXISTING_ALIGNMENT = "updateExistingAlignment";
	public static final String ID_CLAUSE_EXTRACTOR_FORMATTER = "idClauseExtractorFormatter";
	public static final String SKIP_ROWS_WITH_MISSING_SEGMENTS = "skipRowsWithMissingSegments";

	
	private RegexExtractorFormatter idClauseExtractorFormatter = null;
	
	private Boolean ignoreRegexMatchFailures = false;
	private Boolean ignoreMissingSequences = false;
	private Boolean updateExistingMembers = false;
	private Boolean updateExistingAlignment = false;
	private Boolean skipRowsWithMissingSegments = false;


	
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		ignoreRegexMatchFailures = Optional
				.ofNullable(PluginUtils.configureBooleanProperty(configElem, IGNORE_REGEX_MATCH_FAILURES, false))
				.orElse(false);
		ignoreMissingSequences = Optional
				.ofNullable(PluginUtils.configureBooleanProperty(configElem, IGNORE_MISSING_SEQUENCES, false))
				.orElse(false);
		updateExistingMembers = Optional
				.ofNullable(PluginUtils.configureBooleanProperty(configElem, UPDATE_EXISTING_MEMBERS, false))
				.orElse(false);
		updateExistingAlignment = Optional
				.ofNullable(PluginUtils.configureBooleanProperty(configElem, UPDATE_EXISTING_ALIGNMENT, false))
				.orElse(false);
		skipRowsWithMissingSegments = Optional
				.ofNullable(PluginUtils.configureBooleanProperty(configElem, SKIP_ROWS_WITH_MISSING_SEGMENTS, false))
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
		FastaUtils.normalizeFastaBytes(cmdContext, fastaFileBytes);
		
		Alignment alignment = GlueDataObject.create(cmdContext, Alignment.class, Alignment.pkMap(alignmentName), updateExistingAlignment);

		ReferenceSequence refSequence = alignment.getRefSequence();
		if(refSequence != null) {
			throw new FastaAlignmentImporterException(Code.ALIGNMENT_IS_CONSTRAINED, alignmentName, refSequence.getName());
		}
		
		Map<String, DNASequence> sequenceMap = FastaUtils.parseFasta(fastaFileBytes);
		
		List<Map<String, Object>> resultListOfMaps = new ArrayList<Map<String, Object>>();
		
		for(Map.Entry<String, DNASequence> entry: sequenceMap.entrySet()) {
			String fastaID = entry.getKey();

			Sequence foundSequence = findSequence(cmdContext, fastaID, sourceName);
			if(foundSequence == null) {
				continue;
			}
			
			String memberSourceName = foundSequence.getSource().getName();
			String memberSequenceID = foundSequence.getSequenceID();
			
			AlignmentMember almtMember = 
					GlueDataObject.create(cmdContext, AlignmentMember.class, 
					AlignmentMember.pkMap(alignmentName, memberSourceName, memberSequenceID), updateExistingMembers);
			almtMember.setAlignment(alignment);
			almtMember.setSequence(foundSequence);
			List<QueryAlignedSegment> existingSegs = almtMember.getAlignedSegments().stream()
					.map(AlignedSegment::asQueryAlignedSegment)
					.collect(Collectors.toList());
			
			List<QueryAlignedSegment> queryAlignedSegs = null; 

			DNASequence alignmentRowDnaSequence = entry.getValue();
			String alignmentRowAsString = alignmentRowDnaSequence.getSequenceAsString();
			try {
					queryAlignedSegs = 
							findAlignedSegs(cmdContext, foundSequence, existingSegs, 
									alignmentRowAsString, fastaID);
			} catch(FastaAlignmentImporterException faie) {
				if(skipRowsWithMissingSegments && faie.getCode().equals(FastaAlignmentImporterException.Code.SUBSEQUENCE_NOT_FOUND)) {
					String startColumnNumber = faie.getErrorArgs()[0].toString();
					String endColumnNumber = faie.getErrorArgs()[1].toString();
					String fastaId = faie.getErrorArgs()[2].toString();
					
					GlueLogger.getGlueLogger().warning("Skipping alignment row "+fastaId+
							": segment ["+startColumnNumber+", "+endColumnNumber+
							"] is missing in Sequence{sourceName="+memberSourceName+", sequenceID="+memberSequenceID+"}");
					continue;
				} else {
					throw faie;
				}
			}
			
			for(QueryAlignedSegment queryAlignedSeg: queryAlignedSegs) {
				AlignedSegment alignedSegment = GlueDataObject.create(cmdContext, AlignedSegment.class, 
						AlignedSegment.pkMap(alignmentName, memberSourceName, memberSequenceID, 
								queryAlignedSeg.getRefStart(), queryAlignedSeg.getRefEnd(), 
								queryAlignedSeg.getQueryStart(), queryAlignedSeg.getQueryEnd()), false);
				alignedSegment.setAlignmentMember(almtMember);
			}

			AbstractSequenceObject foundSeqObj = foundSequence.getSequenceObject();

			int alignmentRowNonGapNTs = 0;
			for(int i = 0; i < alignmentRowAsString.length(); i++) {
				if(alignmentRowAsString.charAt(i) != '-') {
					alignmentRowNonGapNTs++;
				}
			}
			int coveredAlignmentRowNTs = 0;
			
			int correctCalls = 0;
			for(QueryAlignedSegment queryAlignedSeg: queryAlignedSegs) {
				coveredAlignmentRowNTs += queryAlignedSeg.getCurrentLength();
				int queryStart = queryAlignedSeg.getQueryStart();
				for(int i = 0; i < queryAlignedSeg.getCurrentLength(); i++) {
					char almtRowNT = alignmentRowAsString.charAt((queryAlignedSeg.getRefStart()+i)-1);
					char foundNT = foundSeqObj.getNucleotides(cmdContext, queryStart+i, queryStart+i).charAt(0); 
					if(almtRowNT == foundNT) {
						correctCalls++;
					}
				}
			}
			double alignmentRowCoverage = 0.0;
			double correctCallsFrac = 0.0;
			if(alignmentRowNonGapNTs > 0) {
				alignmentRowCoverage = coveredAlignmentRowNTs / (double) alignmentRowNonGapNTs;
				correctCallsFrac = correctCalls / (double) alignmentRowNonGapNTs;
			}
			
			Map<String, Object> memberResultMap = new LinkedHashMap<String, Object>();
			memberResultMap.put("fastaID", fastaID);
			memberResultMap.put("sourceName", memberSourceName);
			memberResultMap.put("sequenceID", memberSequenceID);
			memberResultMap.put("numSegmentsAdded", new Integer(queryAlignedSegs.size()));
			memberResultMap.put("almtRowCoverage", alignmentRowCoverage * 100);
			memberResultMap.put("correctCalls", correctCallsFrac * 100);
			resultListOfMaps.add(memberResultMap);
		}
		
		cmdContext.commit();
		return new FastaAlignmentImporterResult(resultListOfMaps);
	}


	protected Sequence findSequence(CommandContext cmdContext, String fastaID, String sourceName) {
		String whereClauseString = idClauseExtractorFormatter.matchAndConvert(fastaID);
		if(whereClauseString == null) {
			if(ignoreRegexMatchFailures) {
				return null;
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
		List<Sequence> foundSequences = GlueDataObject.query(cmdContext, Sequence.class, new SelectQuery(Sequence.class, whereClauseExp));
		if(foundSequences.isEmpty()) {
			if(ignoreMissingSequences) {
				return null;
			}
			throw new FastaAlignmentImporterException(Code.NO_SEQUENCE_FOUND, fastaID, whereClauseString);
		}
		if(foundSequences.size() > 1) {
			throw new FastaAlignmentImporterException(Code.MULTIPLE_SEQUENCES_FOUND, fastaID, whereClauseString);
		}
		Sequence foundSequence = foundSequences.get(0);
		return foundSequence;
	}


	protected abstract List<QueryAlignedSegment> findAlignedSegs(CommandContext cmdContext, Sequence foundSequence, 
			List<QueryAlignedSegment> existingSegs, String fastaAlignmentNTs, 
			String fastaID);


	protected static class FastaAlignmentImporterResult extends TableResult {
		public FastaAlignmentImporterResult(List<Map<String, Object>> rowData) {
			super("fastaAlignmentImporterResult", Arrays.asList("fastaID", "sourceName", "sequenceID", "numSegmentsAdded", "almtRowCoverage", "correctCalls"), rowData);
		}
	}

}
