package uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;

import org.biojava.nbio.core.sequence.DNASequence;

import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment.FastaAlignmentExportCommandDelegate.OrderStrategy;
import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.memberSupplier.AbstractMemberSupplier;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.AbstractSequenceObject;
import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;
import uk.ac.gla.cvr.gluetools.core.segments.SegmentUtils;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils.LineFeedStyle;
import freemarker.template.Template;

@PluginClass(elemName="fastaAlignmentExporter")
public class FastaAlignmentExporter extends AbstractFastaAlignmentExporter<FastaAlignmentExporter> {
	
	public FastaAlignmentExporter() {
		super();
		addModulePluginCmdClass(FastaAlignmentExportCommand.class);
		addModulePluginCmdClass(FastaAlignmentWebExportCommand.class);
		
	}

	// no column selector module
	// alignment members already established
	// no order strategy
	public static String webExportAlignment(CommandContext cmdContext, 
			Alignment alignment, 
			List<AlignmentMember> almtMembers, 
			Boolean excludeEmptyRows, 
			Template idTemplate, LineFeedStyle lineFeedStyle) {
		ReferenceSegment minMaxSeg = new ReferenceSegment(1, 1);
		GlueLogger.getGlueLogger().log(Level.INFO, "initialising reference segment");
		ReferenceSequence alignmentRef = alignment.getRefSequence();
		minMaxSeg.setRefEnd(alignmentRef.getSequence().getSequenceObject().getNucleotides(cmdContext).length());
		Map<Map<String,String>, AlmtRowInfo> pkMapToAlmtRowInfo = new LinkedHashMap<Map<String,String>, AlmtRowInfo>();
		createAlignment(cmdContext, excludeEmptyRows, null, alignment, almtMembers, null, minMaxSeg, pkMapToAlmtRowInfo);

		Map<Map<String,String>, DNASequence> memAlmtMap = new LinkedHashMap<Map<String,String>, DNASequence>();
		pkMapToAlmtRowInfo.forEach((pkMap, almtRowInfo) -> {
			memAlmtMap.put(pkMap, almtRowInfo.dnaSequence);
		});
		Map<Map<String,String>, String> pkMapToFastaId = new LinkedHashMap<Map<String,String>, String>();
		for(AlignmentMember almtMember: almtMembers) {
			pkMapToFastaId.put(almtMember.pkMap(), generateFastaId(idTemplate, almtMember));
		}
		GlueLogger.getGlueLogger().log(Level.INFO, "Creating alignment string");
		return createFastaAlignmentString(pkMapToFastaId, memAlmtMap, lineFeedStyle);
	}
	
	public static String exportAlignment(CommandContext cmdContext,
			AbstractMemberSupplier memberSupplier,
			IAlignmentColumnsSelector alignmentColumnsSelector,
			OrderStrategy orderStrategy, 
			Boolean excludeEmptyRows,
			Template idTemplate, LineFeedStyle lineFeedStyle) {

		GlueLogger.getGlueLogger().log(Level.INFO, "Creating alignment");
		Map<Map<String, String>, DNASequence> memberAlignmentMap = exportAlignment(
				cmdContext, alignmentColumnsSelector, excludeEmptyRows, orderStrategy,
				memberSupplier);
		GlueLogger.getGlueLogger().log(Level.INFO, "Alignment created");

		Map<Map<String,String>, String> pkMapToFastaId = new LinkedHashMap<Map<String,String>, String>();
		
		int numMembers = memberSupplier.countMembers(cmdContext);
		int offset = 0;
		int batchSize = 500;
		while(offset < numMembers) {
			List<AlignmentMember> almtMembers = memberSupplier.supplyMembers(cmdContext, offset, batchSize);
			for(AlignmentMember almtMember: almtMembers) {
				pkMapToFastaId.put(almtMember.pkMap(), generateFastaId(idTemplate, almtMember));
			}
			offset += batchSize;
			cmdContext.newObjectContext();
		}
		GlueLogger.getGlueLogger().log(Level.INFO, "Creating alignment string");
		return createFastaAlignmentString(pkMapToFastaId, memberAlignmentMap, lineFeedStyle);
	}

	public static Map<Map<String, String>, DNASequence> exportAlignment(
			CommandContext cmdContext, IAlignmentColumnsSelector alignmentColumnsSelector,
			Boolean excludeEmptyRows, OrderStrategy orderStrategy,
			AbstractMemberSupplier memberSupplier) {
		
		List<ReferenceSegment> featureRefSegs = initFeatureRefSegs(cmdContext, alignmentColumnsSelector);
		ReferenceSegment minMaxSeg = initMinMaxSeg(cmdContext, memberSupplier, featureRefSegs);
		
		int numMembers = memberSupplier.countMembers(cmdContext);
		GlueLogger.getGlueLogger().log(Level.INFO, "processing "+numMembers+" alignment members");
		int offset = 0;
		int processed = 0;
		int batchSize = 500;
		Map<Map<String,String>, AlmtRowInfo> pkMapToAlmtRowInfo = new LinkedHashMap<Map<String,String>, AlmtRowInfo>();

		while(offset < numMembers) {
			Alignment alignment = memberSupplier.supplyAlignment(cmdContext);
			List<AlignmentMember> almtMembers = memberSupplier.supplyMembers(cmdContext, offset, batchSize);
			createAlignment(cmdContext, excludeEmptyRows, alignmentColumnsSelector, alignment, almtMembers, featureRefSegs, minMaxSeg, pkMapToAlmtRowInfo);
			processed += almtMembers.size();
			GlueLogger.getGlueLogger().log(Level.INFO, "processed "+processed+" alignment members");
			offset += batchSize;
			cmdContext.newObjectContext();
		}
		
		GlueLogger.getGlueLogger().log(Level.INFO, "Reordering alignment");
		pkMapToAlmtRowInfo = orderAlmt(pkMapToAlmtRowInfo, orderStrategy);
		
		Map<Map<String,String>, DNASequence> memAlmtMap = new LinkedHashMap<Map<String,String>, DNASequence>();
		pkMapToAlmtRowInfo.forEach((pkMap, almtRowInfo) -> {
			memAlmtMap.put(pkMap, almtRowInfo.dnaSequence);
		});
		return memAlmtMap;
	}

	private static class AlmtRowInfo {
		DNASequence dnaSequence;
		int firstSegmentStart;
	}
	
	private static List<ReferenceSegment> initFeatureRefSegs(CommandContext cmdContext, IAlignmentColumnsSelector alignmentColumnsSelector) {
		if(alignmentColumnsSelector != null) {
			return alignmentColumnsSelector.selectAlignmentColumns(cmdContext);
		}
		return null;
	}
	
	private static ReferenceSegment initMinMaxSeg(CommandContext cmdContext, AbstractMemberSupplier memberSupplier, List<ReferenceSegment> featureRefSegs) {
		ReferenceSegment minMaxSeg = new ReferenceSegment(1, 1);
		GlueLogger.getGlueLogger().log(Level.INFO, "initialising reference segment");
		ReferenceSequence alignmentRef = memberSupplier.supplyAlignment(cmdContext).getRefSequence();
		if(alignmentRef != null) {
			minMaxSeg.setRefEnd(alignmentRef.getSequence().getSequenceObject().getNucleotides(cmdContext).length());
		} else {
			// unconstrained alignment
			int numMembers = memberSupplier.countMembers(cmdContext);
			int offset = 0;
			int batchSize = 500;
			while(offset < numMembers) {
				List<AlignmentMember> almtMembers = memberSupplier.supplyMembers(cmdContext, offset, batchSize);
				for(AlignmentMember almtMember: almtMembers) {
					Integer maxRefEnd = ReferenceSegment.maxRefEnd(almtMember.getAlignedSegments());
					if(maxRefEnd != null) { minMaxSeg.setRefEnd(Math.max(maxRefEnd, minMaxSeg.getRefEnd())); }
				}
				offset += batchSize;
				cmdContext.newObjectContext();
			}
		}
		if(featureRefSegs != null && !featureRefSegs.isEmpty()) {
			minMaxSeg.setRefStart(ReferenceSegment.minRefStart(featureRefSegs));
			minMaxSeg.setRefEnd(ReferenceSegment.maxRefEnd(featureRefSegs));
		}
		return minMaxSeg;
	}
	
	private static void createAlignment(CommandContext cmdContext, Boolean excludeEmptyRows,
			IAlignmentColumnsSelector alignmentColumnsSelector,
			Alignment alignment, List<AlignmentMember> almtMembers, List<ReferenceSegment> featureRefSegs, 
			ReferenceSegment minMaxSeg, Map<Map<String,String>, AlmtRowInfo> pkMapToAlmtRowInfo) {

		Map<Map<String, String>, List<QueryAlignedSegment>> pkMapToQaSegs = 
				new LinkedHashMap<Map<String, String>, List<QueryAlignedSegment>>();
		Map<Map<String, String>, AbstractSequenceObject> pkMapToSeqObj = 
				new LinkedHashMap<Map<String, String>, AbstractSequenceObject>();
		for(AlignmentMember almtMember: almtMembers) {
			Map<String,String> pkMap = almtMember.pkMap();
			List<QueryAlignedSegment> memberQaSegs = almtMember.segmentsAsQueryAlignedSegments();
			if(alignmentColumnsSelector != null) {
				// related reference specified in order to specify feature location
				Alignment tipAlmt = almtMember.getAlignment();
				ReferenceSequence relatedRef = alignment.getRelatedRef(cmdContext, alignmentColumnsSelector.getRelatedRefName());
				memberQaSegs = tipAlmt.translateToRelatedRef(cmdContext, memberQaSegs, relatedRef);
			} else {
				// no feature location but still need to translate to ancestor-constraining reference, 
				// because member is of a descendent alignment.
				if(!alignment.getName().equals(almtMember.getAlignment().getName())) {
					ReferenceSequence acRef2 = alignment.getRefSequence();
					memberQaSegs = almtMember.getAlignment().translateToAncConstrainingRef(cmdContext, memberQaSegs, acRef2);
				}
			}
			if(featureRefSegs != null) {
				memberQaSegs = ReferenceSegment.intersection(memberQaSegs, featureRefSegs, ReferenceSegment.cloneLeftSegMerger());
			}
			pkMapToQaSegs.put(pkMap, memberQaSegs);
			pkMapToSeqObj.put(pkMap, almtMember.getSequence().getSequenceObject());
		}
		addToMemberAlignmentMap(cmdContext, excludeEmptyRows, minMaxSeg, pkMapToQaSegs, pkMapToSeqObj, pkMapToAlmtRowInfo);
	}
	
	

	private static void addToMemberAlignmentMap(CommandContext cmdContext,
			Boolean excludeEmptyRows, ReferenceSegment minMaxSeg,
			Map<Map<String,String>, List<QueryAlignedSegment>> pkMapToQaSegs,
			Map<Map<String,String>, AbstractSequenceObject> pkMapToSeqObj, 
			Map<Map<String,String>, AlmtRowInfo> pkMapToAlmtRowInfo) {
		
		int minRefNt_final = minMaxSeg.getRefStart();
		int maxRefNt_final = minMaxSeg.getRefEnd();

		pkMapToQaSegs.forEach((pkMap, qaSegs) -> {
			
			List<QueryAlignedSegment> truncatedQaSegs = ReferenceSegment.intersection(qaSegs, Arrays.asList(minMaxSeg), ReferenceSegment.cloneLeftSegMerger());
			String memberNTs = pkMapToSeqObj.get(pkMap).getNucleotides(cmdContext);
			StringBuffer alignmentRow = new StringBuffer(maxRefNt_final);
			int ntIndex = minRefNt_final;
			for(QueryAlignedSegment seg: truncatedQaSegs) {
				while(ntIndex < seg.getRefStart()) {
					alignmentRow.append("-");
					ntIndex++;
				}
				alignmentRow.append(SegmentUtils.base1SubString(memberNTs, seg.getQueryStart(), seg.getQueryEnd()));
				ntIndex = seg.getRefEnd()+1;
			}
			while(ntIndex <= maxRefNt_final) {
				alignmentRow.append("-");
				ntIndex++;
			}
			if( (!truncatedQaSegs.isEmpty()) || !excludeEmptyRows) {
				AlmtRowInfo almtRowInfo = new AlmtRowInfo();
				almtRowInfo.dnaSequence = FastaUtils.ntStringToSequence(alignmentRow.toString());
				if(truncatedQaSegs.isEmpty()) {
					almtRowInfo.firstSegmentStart = maxRefNt_final;
				} else {
					almtRowInfo.firstSegmentStart = ReferenceSegment.minRefStart(truncatedQaSegs);
				}
				pkMapToAlmtRowInfo.put(pkMap, almtRowInfo);
			} 
	    });
	}

	

	private static String createFastaAlignmentString(Map<Map<String,String>, String> pkMapToFastaId,
			Map<Map<String,String>, DNASequence> memberAlignmentMap, LineFeedStyle lineFeedStyle) {
		StringBuffer stringBuffer = new StringBuffer();
		memberAlignmentMap.forEach((pkMap, alignmentRow) -> {
			stringBuffer.append(FastaUtils.seqIdCompoundsPairToFasta(pkMapToFastaId.get(pkMap), alignmentRow.getSequenceAsString(), lineFeedStyle));
	    });
		return stringBuffer.toString();
	}
	
	
	private static Map<Map<String,String>, AlmtRowInfo> orderAlmt(Map<Map<String,String>, AlmtRowInfo> alignment, OrderStrategy orderStrategy) {
		if(orderStrategy == null) {
			return alignment;
		}
		Set<Entry<Map<String, String>, AlmtRowInfo>> entrySet = alignment.entrySet();
		ArrayList<Entry<Map<String, String>, AlmtRowInfo>> entryArray = new ArrayList<Entry<Map<String, String>, AlmtRowInfo>>(entrySet);
		Comparator<Entry<Map<String, String>, AlmtRowInfo>> comparator = null;
		switch(orderStrategy) {
		case increasing_start_segment:
			comparator = new Comparator<Entry<Map<String, String>, AlmtRowInfo>>() {
				@Override
				public int compare(Entry<Map<String, String>, AlmtRowInfo> o1, Entry<Map<String, String>, AlmtRowInfo> o2) {
					return Integer.compare(o1.getValue().firstSegmentStart, o2.getValue().firstSegmentStart);
				}
			};
		}
		entryArray.sort(comparator);
		Map<Map<String,String>, AlmtRowInfo> sorted = new LinkedHashMap<Map<String,String>, AlmtRowInfo>();
		entryArray.forEach(mem -> {
			sorted.put(mem.getKey(), mem.getValue());
		});
		return sorted;
	}

	
}
