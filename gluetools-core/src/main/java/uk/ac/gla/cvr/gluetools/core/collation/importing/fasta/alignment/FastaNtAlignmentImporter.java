package uk.ac.gla.cvr.gluetools.core.collation.importing.fasta.alignment;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.biojava.nbio.core.sequence.DNASequence;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignedSegment.AlignedSegment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.AbstractSequenceObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;

public abstract class FastaNtAlignmentImporter<I extends FastaNtAlignmentImporter<I>> extends BaseFastaAlignmentImporter<I> {

	public static final String MIN_ROW_COVERAGE_PERCENT = "minRowCoveragePercent";
	public static final String MIN_ROW_CORRECT_PERCENT = "minRowCorrectPercent";

	private Double minRowCoveragePercent;
	private Double minRowCorrectPercent;

	public FastaNtAlignmentImporter() {
		super();
		addSimplePropertyName(MIN_ROW_CORRECT_PERCENT);
		addSimplePropertyName(MIN_ROW_COVERAGE_PERCENT);
	}

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		minRowCoveragePercent = Optional
				.ofNullable(PluginUtils.configureDoubleProperty(configElem, MIN_ROW_COVERAGE_PERCENT, false))
				.orElse(95.0);
		minRowCorrectPercent = Optional
				.ofNullable(PluginUtils.configureDoubleProperty(configElem, MIN_ROW_CORRECT_PERCENT, false))
				.orElse(95.0);
	}

	public FastaAlignmentImporterResult doPreview(ConsoleCommandContext cmdContext, String fileName, String sourceName) {
		return doImport(cmdContext, fileName, null, sourceName);
	}
	
	public final FastaAlignmentImporterResult doImport(ConsoleCommandContext cmdContext, String fileName, Alignment alignment, String sourceName) {
		byte[] fastaFileBytes = cmdContext.loadBytes(fileName);
		FastaUtils.normalizeFastaBytes(cmdContext, fastaFileBytes);
		
		
		Map<String, DNASequence> sequenceMap = FastaUtils.parseFasta(fastaFileBytes);
		
		List<Map<String, Object>> resultListOfMaps = new ArrayList<Map<String, Object>>();
		
		int alignmentRows = 0;
		
		for(Map.Entry<String, DNASequence> entry: sequenceMap.entrySet()) {
			String fastaID = entry.getKey();

			Sequence foundSequence = findSequence(cmdContext, fastaID, sourceName);
			if(foundSequence == null) {
				continue;
			}
			
			String memberSourceName = foundSequence.getSource().getName();
			String memberSequenceID = foundSequence.getSequenceID();
			
			AlignmentMember almtMember = null; 
			List<QueryAlignedSegment> existingSegs = null;
			if(alignment != null) {
				almtMember = createAlignmentMember(cmdContext, alignment, foundSequence);
				existingSegs = almtMember.getAlignedSegments().stream()
						.map(AlignedSegment::asQueryAlignedSegment)
						.collect(Collectors.toList());
			} else {
				existingSegs = new ArrayList<QueryAlignedSegment>();
			}

			List<QueryAlignedSegment> queryAlignedSegs = null; 

			DNASequence alignmentRowDnaSequence = entry.getValue();
			String alignmentRowAsString = alignmentRowDnaSequence.getSequenceAsString();
			queryAlignedSegs = findAlignedSegs(cmdContext, foundSequence, existingSegs, alignmentRowAsString, fastaID);
			if(queryAlignedSegs == null) {
				// null return value means skip this alignment row. A warning should log the reason.
				continue;
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
			double alignmentRowCoveragePct = 0.0;
			double correctCallsPct = 0.0;
			if(alignmentRowNonGapNTs > 0) {
				alignmentRowCoveragePct = 100.0 * coveredAlignmentRowNTs / (double) alignmentRowNonGapNTs;
				correctCallsPct = 100.0 * correctCalls / (double) coveredAlignmentRowNTs;
			}
			
			if(alignmentRowCoveragePct < minRowCoveragePercent) {
				GlueLogger.getGlueLogger().warning("Skipping row with fasta ID "+fastaID+" row coverage percent "+alignmentRowCoveragePct+" < "+minRowCoveragePercent);
				continue;
			}
			if(correctCallsPct < minRowCorrectPercent) {
				GlueLogger.getGlueLogger().warning("Skipping row with fasta ID "+fastaID+" correct calls percent "+correctCallsPct+" < "+minRowCorrectPercent);
				continue;
			}
			
			if(alignment != null) {
				for(QueryAlignedSegment queryAlignedSeg: queryAlignedSegs) {
					AlignedSegment alignedSegment = GlueDataObject.create(cmdContext, AlignedSegment.class, 
							AlignedSegment.pkMap(alignment.getName(), memberSourceName, memberSequenceID, 
									queryAlignedSeg.getRefStart(), queryAlignedSeg.getRefEnd(), 
									queryAlignedSeg.getQueryStart(), queryAlignedSeg.getQueryEnd()), false);
					alignedSegment.setAlignmentMember(almtMember);
				}
			}
			
			Map<String, Object> memberResultMap = new LinkedHashMap<String, Object>();
			memberResultMap.put("fastaID", fastaID);
			memberResultMap.put("sourceName", memberSourceName);
			memberResultMap.put("sequenceID", memberSequenceID);
			memberResultMap.put("numSegmentsAdded", new Integer(queryAlignedSegs.size()));
			memberResultMap.put("almtRowCoverage", alignmentRowCoveragePct);
			memberResultMap.put("correctCalls", correctCallsPct);
			resultListOfMaps.add(memberResultMap);
			alignmentRows++;
			if(alignmentRows % 25 == 0) {
				log("Imported "+alignmentRows+" alignment rows");
			}
			
		}
		log("Imported "+alignmentRows+" alignment rows");
		
		cmdContext.commit();
		return new FastaAlignmentImporterResult(resultListOfMaps);
	}


	protected abstract List<QueryAlignedSegment> findAlignedSegs(CommandContext cmdContext, Sequence foundSequence, 
			List<QueryAlignedSegment> existingSegs, String fastaAlignmentNTs, 
			String fastaID);


}
