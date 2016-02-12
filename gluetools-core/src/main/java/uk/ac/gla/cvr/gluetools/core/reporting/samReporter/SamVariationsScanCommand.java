package uk.ac.gla.cvr.gluetools.core.reporting.samReporter;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import htsjdk.samtools.SamReader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ProvidedProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.Variation;
import uk.ac.gla.cvr.gluetools.core.reporting.samReporter.SamReporter.RecordsCounter;
import uk.ac.gla.cvr.gluetools.core.segments.AaReferenceSegment;
import uk.ac.gla.cvr.gluetools.core.segments.NtReferenceSegment;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;
import uk.ac.gla.cvr.gluetools.core.segments.SegmentUtils;
import uk.ac.gla.cvr.gluetools.core.transcription.CommandContextTranslator;
import uk.ac.gla.cvr.gluetools.core.transcription.TranslationFormat;
import uk.ac.gla.cvr.gluetools.core.transcription.TranslationUtils;
import uk.ac.gla.cvr.gluetools.core.transcription.Translator;

@CommandClass(
		commandWords={"variation", "scan"}, 
		description = "Scan a SAM/BAM file for variations", 
		docoptUsages = { SamReporterCommand.SAM_REPORTER_CMD_USAGE },
		docoptOptions = { 
				"-i <fileName>, --fileName <fileName>                 SAM/BAM input file",
				"-s <samRefName>, --samRefName <samRefName>           Specific SAM ref sequence",
				"-a <alignmentName>, --alignmentName <alignmentName>  Tip alignment",
				"-r <refName>, --refName <refName>                    GLUE reference name",
				"-f <featureName>, --featureName <featureName>        GLUE feature name"},
		furtherHelp = 
			SamReporterCommand.SAM_REPORTER_CMD_FURTHER_HELP+
			" The variation scan will be limited to variations defined on this feature location.",
		metaTags = {CmdMeta.consoleOnly}	
)
public class SamVariationsScanCommand extends SamReporterCommand<SamVariationsScanResult> 
	implements ProvidedProjectModeCommand{


	@Override
	protected SamVariationsScanResult execute(
				CommandContext cmdContext,
				SamReporter samReporter) {
		Alignment tipAlignment = getTipAlignment(cmdContext);
		ReferenceSequence constrainingRef = getConstrainingRef(tipAlignment);
		ReferenceSequence scannedRef = getScannedRef(cmdContext, tipAlignment);

		FeatureLocation scannedFeatureLoc = getScannedFeatureLoc(cmdContext);
		
		List<Variation> variations = scannedFeatureLoc.getVariations();
		boolean anyAaVariations = variations.stream().anyMatch(var -> var.getTranslationFormat() == TranslationFormat.AMINO_ACID);
		
		List<QueryAlignedSegment> samRefToGlueRefSegsFull = getSamRefToGlueRefSegs(cmdContext, samReporter, tipAlignment, constrainingRef, scannedRef);

		List<ReferenceSegment> featureRefSegs = scannedFeatureLoc.getSegments().stream()
				.map(seg -> seg.asReferenceSegment()).collect(Collectors.toList());

		List<QueryAlignedSegment> samRefToGlueRefSegs = 
				ReferenceSegment.intersection(samRefToGlueRefSegsFull, featureRefSegs, ReferenceSegment.cloneLeftSegMerger());

		Integer codon1Start;

		final Translator translator;
		if(anyAaVariations) {
			codon1Start = scannedFeatureLoc.getCodon1Start(cmdContext);
			translator = new CommandContextTranslator(cmdContext);
		} else {
			codon1Start = null;
			translator = null;
		}

		List<VariationInfo> variationInfos = variations.stream()
				.map(var -> new VariationInfo(codon1Start, var)).collect(Collectors.toList());

		// for efficiency, build a lookup table of GLUE ref NT location to variations
		// that cover that location.
		final TIntObjectMap<List<VariationInfo>> glueRefNtToVariationInfos = new TIntObjectHashMap<List<VariationInfo>>();
		
		for(VariationInfo variationInfo: variationInfos) {
			for(int i = variationInfo.glueRefStartNt; i <= variationInfo.glueRefEndNt; i++) {
				List<VariationInfo> variationInfosAtNt = glueRefNtToVariationInfos.get(i);
				if(variationInfosAtNt == null) {
					variationInfosAtNt = new ArrayList<VariationInfo>();
					glueRefNtToVariationInfos.put(i, variationInfosAtNt);
				}
				variationInfosAtNt.add(variationInfo);
			}
		}
		
		
 		try(SamReader samReader = newSamReader(cmdContext)) {

			SamRecordFilter samRecordFilter = getSamRecordFilter(samReader, samReporter);

			final RecordsCounter recordsCounter = samReporter.new RecordsCounter();

			samReader.forEach(samRecord -> {
				if(!samRecordFilter.recordPasses(samRecord)) {
					return;
				}
				List<QueryAlignedSegment> readToSamRefSegs = getReadToSamRefSegs(samRecord);
				List<QueryAlignedSegment> readToGlueRefSegs = QueryAlignedSegment.translateSegments(readToSamRefSegs, samRefToGlueRefSegs);
				
				readToGlueRefSegs = 
						ReferenceSegment.intersection(readToGlueRefSegs, featureRefSegs, ReferenceSegment.cloneLeftSegMerger());
				
				List<QueryAlignedSegment> readToGlueRefSegsCodonAligned = null; 
				if(anyAaVariations) {
					readToGlueRefSegsCodonAligned = TranslationUtils.truncateToCodonAligned(codon1Start, readToGlueRefSegs);
				}
				
				// collect all variations which cover the NT locations covered by this read.
				Set<VariationInfo> possibleVariationInfos = new LinkedHashSet<VariationInfo>();
				for(QueryAlignedSegment readToGlueRefSeg: readToGlueRefSegs) {
					for(int i = readToGlueRefSeg.getRefStart(); i <= readToGlueRefSeg.getRefEnd(); i++) {
						List<VariationInfo> varInfosAtNt = glueRefNtToVariationInfos.get(i);
						if(varInfosAtNt != null) {
							possibleVariationInfos.addAll(varInfosAtNt);
						}
					}
				}
				
				// filter these down to the set that are actually covered by the read.
				List<VariationInfo> readCoveredVariationInfos = new LinkedList<VariationInfo>();
				for(VariationInfo variationInfo: possibleVariationInfos) {
					if(variationInfo.variation.getTranslationFormat() == TranslationFormat.AMINO_ACID) {
						if(ReferenceSegment.covers(readToGlueRefSegsCodonAligned, variationInfo.refSegsNT)) {
							readCoveredVariationInfos.add(variationInfo);
						}
					} else {
						if(ReferenceSegment.covers(readToGlueRefSegs, variationInfo.refSegsNT)) {
							readCoveredVariationInfos.add(variationInfo);
						}
					}
				}

				// get the read string
				final String readString = samRecord.getReadString().toUpperCase();
				
				// map it to NtReferenceSegments
				List<NtReferenceSegment> readSegs = readToGlueRefSegs.stream()
						.map(readSeg -> new NtReferenceSegment(readSeg.getRefStart(), readSeg.getRefEnd(), 
								SegmentUtils.base1SubString(readString, readSeg.getQueryStart(), readSeg.getQueryEnd())))
						.collect(Collectors.toList());
				
				// translate it if necessary.
				List<AaReferenceSegment> translatedReadSegs = null;
				if(anyAaVariations) {
					translatedReadSegs = new ArrayList<AaReferenceSegment>();
					for(QueryAlignedSegment readSeg: readToGlueRefSegsCodonAligned) {
						CharSequence nts = SegmentUtils.base1SubString(readString, readSeg.getQueryStart(), readSeg.getQueryEnd());
						String segAAs = translator.translate(nts);
						AaReferenceSegment translatedReadSeg = new AaReferenceSegment(
								TranslationUtils.getCodon(codon1Start, readSeg.getRefStart()), 
								TranslationUtils.getCodon(codon1Start, readSeg.getRefEnd()), 
								segAAs);
						translatedReadSegs.add(translatedReadSeg);
					}
				}
				
				for(VariationInfo variationInfo: readCoveredVariationInfos) {
					String variationCandidateString;
					if(variationInfo.variation.getTranslationFormat() == TranslationFormat.AMINO_ACID) {
						List<AaReferenceSegment> readVariationAAs = 
								ReferenceSegment.intersection(translatedReadSegs, variationInfo.refSegsAA, ReferenceSegment.cloneLeftSegMerger());
						variationCandidateString = String.join("", 
								readVariationAAs.stream().map(seg -> seg.getAminoAcids()).collect(Collectors.toList()));
					} else {
						List<NtReferenceSegment> readVariationNTs = 
								ReferenceSegment.intersection(readSegs, variationInfo.refSegsNT, ReferenceSegment.cloneLeftSegMerger());
						variationCandidateString = String.join("", 
								readVariationNTs.stream().map(seg -> seg.getNucleotides()).collect(Collectors.toList()));						
					}
					if(variationInfo.variation.getRegexPattern().matcher(variationCandidateString).find()) {
						variationInfo.readsConfirmedPresent++;
					} else {
						variationInfo.readsConfirmedAbsent++;
					}
				}
				
				recordsCounter.processedRecord();
				recordsCounter.logRecordsProcessed();
			});
			recordsCounter.logTotalRecordsProcessed();

		} catch (IOException e) {
			throw new RuntimeException(e);
		}


 		
		
		List<Map<String, Object>> rowData = new ArrayList<Map<String, Object>>();
		for(VariationInfo variationInfo : variationInfos) {
			Map<String, Object> row = new LinkedHashMap<String, Object>();
			row.put(SamVariationsScanResult.VARIATION_NAME, variationInfo.variation.getName());
			row.put(SamVariationsScanResult.READS_CONFIRMED_PRESENT, variationInfo.readsConfirmedPresent);
			row.put(SamVariationsScanResult.READS_CONFIRMED_ABSENT, variationInfo.readsConfirmedAbsent);
			rowData.add(row);
		}

		return new SamVariationsScanResult(rowData);

		


}

	
	private class VariationInfo {
		List<ReferenceSegment> refSegsNT;
		List<ReferenceSegment> refSegsAA;
		Variation variation;
		int readsConfirmedPresent = 0;
		int readsConfirmedAbsent = 0;
		int glueRefStartNt;
		int glueRefEndNt;
		
		public VariationInfo(Integer codon1Start, Variation variation) {
			super();
			this.variation = variation;
			if(variation.getTranslationFormat() == TranslationFormat.AMINO_ACID) {
				glueRefStartNt = TranslationUtils.getNt(codon1Start, variation.getRefStart());
				glueRefEndNt = TranslationUtils.getNt(codon1Start, variation.getRefEnd());
				this.refSegsAA = Arrays.asList(new ReferenceSegment(variation.getRefStart(), variation.getRefEnd()));
			} else {
				glueRefStartNt = variation.getRefStart();
				glueRefEndNt = variation.getRefEnd();
			}
			this.refSegsNT = Arrays.asList(new ReferenceSegment(glueRefStartNt, glueRefEndNt));

		}
		
	}
	
	
	
	
	@CompleterClass
	public static class Completer extends SamReporterCmdCompleter {}






	
}
