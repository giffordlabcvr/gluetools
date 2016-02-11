package uk.ac.gla.cvr.gluetools.core.reporting.samReporter;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import htsjdk.samtools.SamReader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ProvidedProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;
import uk.ac.gla.cvr.gluetools.core.segments.SegmentUtils;

@CommandClass(
		commandWords={"nucleotides"}, 
		description = "Extract nucleotides from a SAM/BAM file", 
		docoptUsages = { SamNucleotidesCommand.SAM_REPORTER_CMD_USAGE },
		docoptOptions = { 
				"-i <fileName>, --fileName <fileName>                 SAM/BAM input file",
				"-s <samRefName>, --samRefName <samRefName>           Specific SAM ref sequence",
				"-a <alignmentName>, --alignmentName <alignmentName>  Tip alignment",
				"-r <refName>, --refName <refName>                    GLUE reference name",
				"-f <featureName>, --featureName <featureName>        GLUE feature name"},
		furtherHelp = SamNucleotidesCommand.SAM_REPORTER_CMD_FURTHER_HELP+
		    " The nucleotides will be limited to variations defined on this feature location.",
		metaTags = {CmdMeta.consoleOnly}	
)
public class SamNucleotidesCommand extends SamReporterCommand<SamNucleotidesResult> implements ProvidedProjectModeCommand{

	@Override
	protected SamNucleotidesResult execute(
				CommandContext cmdContext,
				SamReporter samReporter) {
		Alignment tipAlignment = getTipAlignment(cmdContext);
		ReferenceSequence constrainingRef = getConstrainingRef(tipAlignment);
		ReferenceSequence scannedRef = getScannedRef(cmdContext, tipAlignment);

		List<QueryAlignedSegment> samRefToGlueRefSegsFull = getSamRefToGlueRefSegs(cmdContext, samReporter, tipAlignment, constrainingRef, scannedRef);
		
		FeatureLocation scannedFeatureLoc = getScannedFeatureLoc(cmdContext);
		List<ReferenceSegment> featureRefSegs = scannedFeatureLoc.getSegments().stream()
			.map(seg -> seg.asReferenceSegment()).collect(Collectors.toList());

		List<QueryAlignedSegment> samRefToGlueRefSegs = 
				ReferenceSegment.intersection(samRefToGlueRefSegsFull, featureRefSegs, ReferenceSegment.cloneLeftSegMerger());
		
		int summedLengths = samRefToGlueRefSegs.stream().mapToInt(QueryAlignedSegment::getCurrentLength).sum();
		
		final TIntObjectMap<RefNtInfo> glueRefNtToInfo = new TIntObjectHashMap<RefNtInfo>(summedLengths);
		try(SamReader samReader = newSamReader(cmdContext)) {
			SamRecordFilter samRecordFilter = getSamRecordFilter(samReader, samReporter);

			final IntHolder records = new IntHolder();
			
			samReader.forEach(samRecord -> {
				if(!samRecordFilter.recordPasses(samRecord)) {
					return;
				}
				List<QueryAlignedSegment> readToSamRefSegs = getReadToSamRefSegs(samRecord);
				List<QueryAlignedSegment> readToGlueRefSegs = QueryAlignedSegment.translateSegments(readToSamRefSegs, samRefToGlueRefSegs);
				final String readString = samRecord.getReadString().toUpperCase();
				for(QueryAlignedSegment readToGlueRefSeg: readToGlueRefSegs) {
					
					int readNt = readToGlueRefSeg.getQueryStart();
					for(int glueRefNt = readToGlueRefSeg.getRefStart(); glueRefNt <= readToGlueRefSeg.getRefEnd(); glueRefNt++) {
						RefNtInfo refNtInfo = glueRefNtToInfo.get(glueRefNt);
						if(refNtInfo == null) {
							refNtInfo = new RefNtInfo();
							glueRefNtToInfo.put(glueRefNt, refNtInfo);
						}
						char readChar = SegmentUtils.base1Char(readString, readNt);
	        			if(readChar == 'A') {
	        				refNtInfo.readsWithA++;
	        			} else if(readChar == 'C') {
	        				refNtInfo.readsWithC++;
	        			} else if(readChar == 'G') {
	        				refNtInfo.readsWithG++;
	        			} else if(readChar == 'T') {
	        				refNtInfo.readsWithT++;
	        			}
						readNt++;
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
		
		for(QueryAlignedSegment seg: samRefToGlueRefSegs) {
			for(int i = seg.getRefStart(); i <= seg.getRefEnd(); i++) {
				RefNtInfo refNtInfo = glueRefNtToInfo.get(i);
				int refToQueryOffset = seg.getReferenceToQueryOffset();
				if(refNtInfo != null) {
					Map<String, Object> row = new LinkedHashMap<String, Object>();
					row.put(SamNucleotidesResult.GLUE_REFERENCE_NT, i);
					row.put(SamNucleotidesResult.SAM_REFERENCE_NT, i+refToQueryOffset);
					row.put(SamNucleotidesResult.READS_WITH_A, refNtInfo.readsWithA);
					row.put(SamNucleotidesResult.READS_WITH_C, refNtInfo.readsWithC);
					row.put(SamNucleotidesResult.READS_WITH_G, refNtInfo.readsWithG);
					row.put(SamNucleotidesResult.READS_WITH_T, refNtInfo.readsWithT);
					rowData.add(row);
				}
			}
		}
		
		return new SamNucleotidesResult(rowData);
		
	}
	
	private class RefNtInfo {
		int readsWithA;
		int readsWithC;
		int readsWithT;
		int readsWithG;
	}

	@CompleterClass
	public static class Completer extends SamReporterCmdCompleter {}






	
}
