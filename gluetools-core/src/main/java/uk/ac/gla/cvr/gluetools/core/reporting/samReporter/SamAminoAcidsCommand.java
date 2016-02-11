package uk.ac.gla.cvr.gluetools.core.reporting.samReporter;

import gnu.trove.map.TCharIntMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TCharIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.procedure.TCharIntProcedure;
import htsjdk.samtools.SamReader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ProvidedProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.feature.Feature;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;
import uk.ac.gla.cvr.gluetools.core.segments.SegmentUtils;
import uk.ac.gla.cvr.gluetools.core.transcription.CommandContextTranslator;
import uk.ac.gla.cvr.gluetools.core.transcription.TranslationUtils;
import uk.ac.gla.cvr.gluetools.core.transcription.Translator;

@CommandClass(
		commandWords={"amino-acids"}, 
		description = "Translate AAs in a SAM/BAM file", 
		docoptUsages = { SamReporterCommand.SAM_REPORTER_CMD_USAGE },
		docoptOptions = { 
				"-i <fileName>, --fileName <fileName>                 SAM/BAM input file",
				"-s <samRefName>, --samRefName <samRefName>           Specific SAM ref sequence",
				"-a <alignmentName>, --alignmentName <alignmentName>  Tip alignment",
				"-r <refName>, --refName <refName>                    GLUE reference name",
				"-f <featureName>, --featureName <featureName>        GLUE feature name"},
		furtherHelp = SamNucleotidesCommand.SAM_REPORTER_CMD_FURTHER_HELP+
			"The translated amino acids will be limited to this feature location.",
		metaTags = {CmdMeta.consoleOnly}	
)
public class SamAminoAcidsCommand extends SamReporterCommand<SamAminoAcidsResult> 
	implements ProvidedProjectModeCommand{


	@Override
	protected SamAminoAcidsResult execute(
				CommandContext cmdContext,
				SamReporter samReporter) {

		Alignment tipAlignment = getTipAlignment(cmdContext);
		ReferenceSequence constrainingRef = getConstrainingRef(tipAlignment);
		ReferenceSequence scannedRef = getScannedRef(cmdContext, tipAlignment);

		
		FeatureLocation scannedFeatureLoc = getScannedFeatureLoc(cmdContext);
		
		Feature scannedFeature = scannedFeatureLoc.getFeature();
		if(!scannedFeature.codesAminoAcids()) {
			throw new SamReporterCommandException(SamReporterCommandException.Code.FEATURE_DOES_NOT_CODE_AMINO_ACIDS, scannedFeature.getName());
		}
		
		List<QueryAlignedSegment> samRefToGlueRefSegsFull = getSamRefToGlueRefSegs(cmdContext, samReporter, tipAlignment, constrainingRef, scannedRef);
		
		List<ReferenceSegment> featureRefSegs = scannedFeatureLoc.getSegments().stream()
			.map(seg -> seg.asReferenceSegment()).collect(Collectors.toList());

		List<QueryAlignedSegment> samRefToGlueRefSegs = 
				ReferenceSegment.intersection(samRefToGlueRefSegsFull, featureRefSegs, ReferenceSegment.cloneLeftSegMerger());
		

		Integer codon1Start = scannedFeatureLoc.getCodon1Start(cmdContext);

		List<QueryAlignedSegment> samRefToGlueRefSegsCodonAligned = TranslationUtils.truncateToCodonAligned(codon1Start, samRefToGlueRefSegs);

		List<Integer> mappedCodons = new LinkedList<Integer>();
		final TIntObjectMap<RefCodonInfo> glueRefCodonToInfo = new TIntObjectHashMap<RefCodonInfo>();
		
		// prepopulate the results map with empty RefCodonInfo objects.
		for(QueryAlignedSegment samRefToGlueRefSeg: samRefToGlueRefSegsCodonAligned) {
			int samRefNt = samRefToGlueRefSeg.getQueryStart();
			int codon = TranslationUtils.getCodon(codon1Start, samRefToGlueRefSeg.getRefStart());
			while(samRefNt <= samRefToGlueRefSeg.getQueryEnd()) {
				mappedCodons.add(codon);
				RefCodonInfo refCodonInfo = new RefCodonInfo();
				refCodonInfo.samRefNT = samRefNt;
				glueRefCodonToInfo.put(codon, refCodonInfo);
				codon++;
				samRefNt += 3;
			}
		}

		final Translator translator = new CommandContextTranslator(cmdContext);
		
		
		try(SamReader samReader = newSamReader(cmdContext)) {
			
			SamRecordFilter samRecordFilter = getSamRecordFilter(samReader, samReporter);

	        final IntHolder records = new IntHolder();
			
			samReader.forEach(samRecord -> {
				if(!samRecordFilter.recordPasses(samRecord)) {
					return;
				}
				List<QueryAlignedSegment> readToSamRefSegs = getReadToSamRefSegs(samRecord);
				List<QueryAlignedSegment> readToGlueRefSegs = QueryAlignedSegment.translateSegments(readToSamRefSegs, samRefToGlueRefSegs);
				
				List<QueryAlignedSegment> readToGlueRefSegsCodonAligned = TranslationUtils.truncateToCodonAligned(codon1Start, readToGlueRefSegs);

				final String readString = samRecord.getReadString().toUpperCase();

				for(QueryAlignedSegment readToGlueRefSeg: readToGlueRefSegsCodonAligned) {
					CharSequence nts = SegmentUtils.base1SubString(readString, readToGlueRefSeg.getQueryStart(), readToGlueRefSeg.getQueryEnd());
					String segAAs = translator.translate(nts);
					int codon = TranslationUtils.getCodon(codon1Start, readToGlueRefSeg.getRefStart());
					for(int i = 0; i < segAAs.length(); i++) {
						char segAA = segAAs.charAt(i);
						RefCodonInfo refCodonInfo = glueRefCodonToInfo.get(codon);
						refCodonInfo.addAaRead(segAA);
						codon++;
					}
				}
				records.x++;
				if(records.x % 10000 == 0) {
					samReporter.log("Processed "+records.x+" reads aligned to SAM reference");
				}
			});
			samReporter.log("Processed "+records.x+" reads aligned to SAM reference");

		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
		List<Map<String, Object>> rowData = new ArrayList<Map<String, Object>>();
		
		for(Integer codon: mappedCodons) {
			RefCodonInfo refCodonInfo = glueRefCodonToInfo.get(codon);
			refCodonInfo.aaToReadCount.forEachEntry(new TCharIntProcedure() {
				@Override
				public boolean execute(char aminoAcid, int numReads) {
					Map<String, Object> row = new LinkedHashMap<String, Object>();
					row.put(SamAminoAcidsResult.GLUE_REFERENCE_CODON, codon);
					row.put(SamAminoAcidsResult.SAM_REFERENCE_BASE, refCodonInfo.samRefNT);
					row.put(SamAminoAcidsResult.AMINO_ACID, new String(new char[]{aminoAcid}));
					row.put(SamAminoAcidsResult.READS_WITH_AA, numReads);
					rowData.add(row);
					return true;
				}
			});
		}
		
		return new SamAminoAcidsResult(rowData);
		
	}


	@CompleterClass
	public static class Completer extends SamReporterCmdCompleter {}


	private class RefCodonInfo {
		private int samRefNT;
		TCharIntMap aaToReadCount = new TCharIntHashMap();
		public void addAaRead(char aaChar) {
			aaToReadCount.adjustOrPutValue(aaChar, 1, 1);
		}
	}




	
}
