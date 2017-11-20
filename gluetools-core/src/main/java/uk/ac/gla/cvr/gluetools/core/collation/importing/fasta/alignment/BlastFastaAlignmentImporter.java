package uk.ac.gla.cvr.gluetools.core.collation.importing.fasta.alignment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;
import uk.ac.gla.cvr.gluetools.programs.blast.BlastResult;
import uk.ac.gla.cvr.gluetools.programs.blast.BlastRunner;
import uk.ac.gla.cvr.gluetools.programs.blast.BlastRunner.BlastType;
import uk.ac.gla.cvr.gluetools.programs.blast.BlastUtils;
import uk.ac.gla.cvr.gluetools.programs.blast.dbManager.BlastDbManager;
import uk.ac.gla.cvr.gluetools.programs.blast.dbManager.TemporarySingleSeqBlastDB;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils.LineFeedStyle;

@PluginClass(elemName="blastFastaAlignmentImporter", 
		description="Imports a nucleotide alignment from a FASTA file, using BLAST to identify correct Sequence coordinates")
public class BlastFastaAlignmentImporter extends FastaNtAlignmentImporter<BlastFastaAlignmentImporter> {

	private BlastRunner blastRunner = new BlastRunner();
	

	public BlastFastaAlignmentImporter() {
		super();
		addModulePluginCmdClass(BlastFastaAlignmentImporterImportCommand.class);
		addModulePluginCmdClass(BlastFastaAlignmentImporterPreviewCommand.class);
	}

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		Element blastRunnerElem = PluginUtils.findConfigElement(configElem, "blastRunner");
		if(blastRunnerElem != null) {
			PluginFactory.configurePlugin(pluginConfigContext, blastRunnerElem, blastRunner);
		}
	}

	@Override
	public List<QueryAlignedSegment> findAlignedSegs(CommandContext cmdContext, Sequence foundSequence, 
			List<QueryAlignedSegment> existingSegs, String alignmentRowNTs, 
			List<ReferenceSegment> navigationRegion) {
		if(navigationRegion.isEmpty()) {
			return Collections.emptyList();
		}
		
		// remove alignment gaps so that BLAST does not have to jump them. 
		// But remember the mapping so we can apply it later.
		// In these segments incoming row is the reference, gapless is the query
		List<QueryAlignedSegment> gaplessToIncomingRow = new ArrayList<QueryAlignedSegment>();
		QueryAlignedSegment currentSegment = null;
		StringBuffer alignmentRowNtGapless = new StringBuffer();
		int incomingRowIndex = 1;
		int gaplessIndex = 1;
		for(int i = 0; i < alignmentRowNTs.length(); i++) {
			char alignmentRowChar = alignmentRowNTs.charAt(i);
			if(alignmentRowChar == '-') {
				if(currentSegment != null) {
					currentSegment = null;
				}
			} else {
				alignmentRowNtGapless.append(alignmentRowChar);
				if(currentSegment == null) {
					currentSegment = new QueryAlignedSegment(incomingRowIndex, incomingRowIndex, gaplessIndex, gaplessIndex);
					gaplessToIncomingRow.add(currentSegment);
				} else {
					currentSegment.setRefEnd(incomingRowIndex);
					currentSegment.setQueryEnd(gaplessIndex);
				}
				gaplessIndex++;
			}
			incomingRowIndex++;
		}
		
		byte[] gaplessFastaBytes = FastaUtils.seqIdCompoundsPairToFasta("alignmentRowNTs", 
				alignmentRowNtGapless.toString(), LineFeedStyle.LF).getBytes();

		Integer navRegionStart = ReferenceSegment.minRefStart(navigationRegion);
		Integer navRegionEnd = ReferenceSegment.maxRefEnd(navigationRegion);
		
		CharSequence foundSequenceNTs = foundSequence.getSequenceObject().getNucleotides(cmdContext, navRegionStart, navRegionEnd);
		BlastDbManager blastDbManager = BlastDbManager.getInstance();

		String uuid = UUID.randomUUID().toString();
		List<BlastResult> blastResults = null;
		try {
			TemporarySingleSeqBlastDB tempBlastDB = 
					blastDbManager.createTempSingleSeqBlastDB(cmdContext, uuid, "glueSequenceRef", foundSequenceNTs.toString());
			blastResults = blastRunner.executeBlast(cmdContext, BlastType.BLASTN, tempBlastDB, gaplessFastaBytes);
		} finally {
			blastDbManager.removeTempSingleSeqBlastDB(cmdContext, uuid);
		}
		Map<String, List<QueryAlignedSegment>> blastResultsToAlignedSegmentsMap = 
				BlastUtils.blastNResultsToAlignedSegmentsMap("glueSequenceRef", blastResults, null);
		List<QueryAlignedSegment> blastAlignedSegments = blastResultsToAlignedSegmentsMap.get("alignmentRowNTs");
		int navRegionOffset = navRegionStart-1;
		if(navRegionOffset != 0) {
			for(QueryAlignedSegment blastAlignedSegment: blastAlignedSegments) {
				blastAlignedSegment.translateRef(navRegionOffset);
			}
		}		
		
		List<QueryAlignedSegment> foundSeqToGapless = new ArrayList<QueryAlignedSegment>();

		if(blastAlignedSegments != null) {
			for(QueryAlignedSegment queryAlignedSegment: blastAlignedSegments) {
				foundSeqToGapless.add(queryAlignedSegment.invert());
			}
		}
		List<QueryAlignedSegment> foundSeqToIncomingRow = QueryAlignedSegment.translateSegments(foundSeqToGapless, gaplessToIncomingRow);
		return foundSeqToIncomingRow;
	}

	
	
	
	
}
