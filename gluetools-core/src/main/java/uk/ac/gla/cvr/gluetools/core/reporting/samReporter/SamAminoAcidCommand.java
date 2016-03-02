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

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ProvidedProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.feature.Feature;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.reporting.samReporter.SamReporter.RecordsCounter;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;
import uk.ac.gla.cvr.gluetools.core.segments.SegmentUtils;
import uk.ac.gla.cvr.gluetools.core.transcription.CommandContextTranslator;
import uk.ac.gla.cvr.gluetools.core.transcription.TranslationUtils;
import uk.ac.gla.cvr.gluetools.core.transcription.Translator;

@CommandClass(
		commandWords={"amino-acid"}, 
		description = "Translate amino acids in a SAM/BAM file", 
		docoptUsages = { SamReporterCommand.SAM_REPORTER_CMD_USAGE },
		docoptOptions = { 
				"-i <fileName>, --fileName <fileName>             SAM/BAM input file",
				"-s <samRefName>, --samRefName <samRefName>       Specific SAM ref sequence",
				"-a, --autoAlign                                  Auto-align to tip alignment",
				"-m, --specificMember                             Specify tip alignment member",
				"-r <refName>, --refName <refName>                GLUE reference name",
				"-f <featureName>, --featureName <featureName>    GLUE feature name"},
		furtherHelp = 
			SamNucleotideCommand.SAM_REPORTER_CMD_FURTHER_HELP+
			"\nThe translated amino acids will be limited to the specified feature location.",
		metaTags = {CmdMeta.consoleOnly}	
)
public class SamAminoAcidCommand extends SamReporterCommand<SamAminoAcidResult> 
	implements ProvidedProjectModeCommand{

	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
	}


	@Override
	protected SamAminoAcidResult execute(
				CommandContext cmdContext,
				SamReporter samReporter) {

		AlignmentMember tipAlignmentMember = getTipAlignmentMember(cmdContext, samReporter);
		Alignment tipAlignment = getTipAlignment(cmdContext, tipAlignmentMember);
		ReferenceSequence constrainingRef = tipAlignment.getConstrainingRef();
		ReferenceSequence ancConstrainingRef = tipAlignment.getAncConstrainingRef(cmdContext, getReferenceName(samReporter));

		
		FeatureLocation scannedFeatureLoc = getScannedFeatureLoc(cmdContext, samReporter);
		
		Feature scannedFeature = scannedFeatureLoc.getFeature();
		scannedFeature.checkCodesAminoAcids();
		
		List<QueryAlignedSegment> samRefToGlueRefSegsFull = getSamRefToGlueRefSegs(cmdContext, samReporter, tipAlignmentMember, constrainingRef, ancConstrainingRef);
		
		List<ReferenceSegment> featureRefSegs = scannedFeatureLoc.getSegments().stream()
			.map(seg -> seg.asReferenceSegment()).collect(Collectors.toList());

		List<QueryAlignedSegment> samRefToGlueRefSegs = 
				ReferenceSegment.intersection(samRefToGlueRefSegsFull, featureRefSegs, ReferenceSegment.cloneLeftSegMerger());
		

		Integer codon1Start = scannedFeatureLoc.getCodon1Start(cmdContext);

		List<QueryAlignedSegment> samRefToGlueRefSegsCodonAligned = TranslationUtils.truncateToCodonAligned(codon1Start, samRefToGlueRefSegs);

		List<Integer> mappedSamRefNts = new LinkedList<Integer>();
		final TIntObjectMap<RefCodonInfo> samRefNtToCodonInfo = new TIntObjectHashMap<RefCodonInfo>();
		
		// prepopulate the results map with empty RefCodonInfo objects.
		for(QueryAlignedSegment samRefToGlueRefSeg: samRefToGlueRefSegsCodonAligned) {
			int samRefNt = samRefToGlueRefSeg.getQueryStart();
			while(samRefNt <= samRefToGlueRefSeg.getQueryEnd()) {
				RefCodonInfo refCodonInfo = new RefCodonInfo();
				refCodonInfo.samRefNT = samRefNt;
				samRefNtToCodonInfo.put(samRefNt, refCodonInfo);
				mappedSamRefNts.add(samRefNt);
				samRefNt += 3;
			}
		}

		final Translator translator = new CommandContextTranslator(cmdContext);
		
		
		try(SamReader samReader = newSamReader(cmdContext)) {
			
			SamRecordFilter samRecordFilter = getSamRecordFilter(samReader, samReporter);

	        final RecordsCounter recordsCounter = samReporter.new RecordsCounter();
			
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
						RefCodonInfo refCodonInfo = samRefNtToCodonInfo.get(codon);
						refCodonInfo.addAaRead(segAA);
						codon++;
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
		
		for(Integer samRefNt: mappedSamRefNts) {
			RefCodonInfo refCodonInfo = samRefNtToCodonInfo.get(samRefNt);
			refCodonInfo.aaToReadCount.forEachEntry(new TCharIntProcedure() {
				@Override
				public boolean execute(char aminoAcid, int numReads) {
					Map<String, Object> row = new LinkedHashMap<String, Object>();
					row.put(SamAminoAcidResult.SAM_REF_NT, refCodonInfo.samRefNT);
					row.put(SamAminoAcidResult.AMINO_ACID, new String(new char[]{aminoAcid}));
					row.put(SamAminoAcidResult.READS_WITH_AA, numReads);
					row.put(SamAminoAcidResult.PERCENT_AA_READS, 
							100.0 * numReads / (double) refCodonInfo.totalReadsAtCodon);
					rowData.add(row);
					return true;
				}
			});
		}
		
		return new SamAminoAcidResult(rowData);
		
	}


	@CompleterClass
	public static class Completer extends SamReporterCmdCompleter {}


	private class RefCodonInfo {
		private int samRefNT;
		private int totalReadsAtCodon = 0;
		TCharIntMap aaToReadCount = new TCharIntHashMap();
		public void addAaRead(char aaChar) {
			aaToReadCount.adjustOrPutValue(aaChar, 1, 1);
			totalReadsAtCodon++;
		}
	}




	
}
