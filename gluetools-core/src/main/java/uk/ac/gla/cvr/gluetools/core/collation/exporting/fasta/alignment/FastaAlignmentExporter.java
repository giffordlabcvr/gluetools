package uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.apache.cayenne.exp.Expression;
import org.biojava.nbio.core.sequence.DNASequence;

import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.FastaExporterException;
import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment.FastaAlignmentExportCommandDelegate.OrderStrategy;
import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.memberSupplier.AbstractMemberSupplier;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CommandException.Code;
import uk.ac.gla.cvr.gluetools.core.command.project.alignment.AlignmentListMemberCommand;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.AbstractSequenceObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.segments.AllColumnsAlignment;
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

	public static String exportAlignment(CommandContext cmdContext,
			AbstractMemberSupplier memberSupplier,
			IAlignmentColumnsSelector alignmentColumnsSelector,
			OrderStrategy orderStrategy, Boolean includeAllColumns, Integer minColUsage,
			Boolean excludeEmptyRows,
			Template idTemplate, LineFeedStyle lineFeedStyle) {

		GlueLogger.getGlueLogger().log(Level.INFO, "Creating alignment");
		Map<Map<String, String>, DNASequence> memberAlignmentMap = exportAlignment(
				cmdContext, alignmentColumnsSelector, includeAllColumns, minColUsage, excludeEmptyRows, orderStrategy,
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
			Boolean includeAllColumns, Integer minColUsage,
			Boolean excludeEmptyRows, OrderStrategy orderStrategy,
			AbstractMemberSupplier memberSupplier) {
		
		ReferenceSequence refSequence = memberSupplier.supplyAlignment(cmdContext).getRefSequence();
		if(refSequence == null && includeAllColumns) {
			throw new FastaExporterException(uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.FastaExporterException.Code.CANNOT_SPECIFY_INCLUDE_ALL_COLUMNS_FOR_UNCONSTRAINED_ALIGNMENT, 
					memberSupplier.supplyAlignment(cmdContext).getName());
		}

		Map<Map<String, String>, AlmtRowInfo> pkMapToAlmtRowInfo;
		
		if(includeAllColumns) {
			throw new RuntimeException("includeAllColumns option deprecated?");
			
			/* memAlmtMap = createAlignmentIncludeAllColumns(cmdContext, 
					alignmentColumnsSelector, memberSupplier, minColUsage); */

		} else {
			pkMapToAlmtRowInfo = createAlignment(cmdContext, alignmentColumnsSelector, memberSupplier);
		}
		GlueLogger.getGlueLogger().log(Level.INFO, "Reordering alignment");
		pkMapToAlmtRowInfo = orderAlmt(pkMapToAlmtRowInfo, orderStrategy);
		
		Map<Map<String,String>, DNASequence> memAlmtMap = new LinkedHashMap<Map<String,String>, DNASequence>();
		pkMapToAlmtRowInfo.forEach((pkMap, almtRowInfo) -> {
			if(excludeEmptyRows && almtRowInfo.emptyRow) {
				return;
			}
			memAlmtMap.put(pkMap, almtRowInfo.dnaSequence);
		});
		return memAlmtMap;
	}

	private static class AlmtRowInfo {
		DNASequence dnaSequence;
		boolean emptyRow;
		int firstSegmentStart;
	}
	

	private static void createAlignmentIncludeAllColumns(CommandContext cmdContext,
			IAlignmentColumnsSelector alignmentColumnsSelector,
			Alignment alignment, List<AlignmentMember> almtMembers,
			ReferenceSegment minMaxSeg,
			Map<Map<String, String>, List<QueryAlignedSegment>> pkMapToQaSegs,
			Map<Map<String, String>, AbstractSequenceObject> pkMapToSeqObj, 
			Integer minColUsage) {

		AllColumnsAlignment<Key> allColsAlmt = null;

		// ensure all necessary references are in the alignment (we will ignore them later).
		Map<String, ReferenceSequence> includedRefs = new LinkedHashMap<String, ReferenceSequence>();

		for(AlignmentMember almtMember: almtMembers) {
			List<Alignment> ancestors = almtMember.getAlignment().getAncestors();
			// process ancestors in reverse order to ensure parent is added before child.
			for(int i = ancestors.size()-1; i >= 0; i--) {
				Alignment ancestorAlmt = ancestors.get(i);
				ReferenceSequence ancRef = ancestorAlmt.getRefSequence();
				if(!includedRefs.containsKey(ancRef.getName())) {
					Sequence refSeqSeq = ancRef.getSequence();
					Alignment parentAlmt = ancestorAlmt.getParent();
					AbstractSequenceObject seqObj = ancRef.getSequence().getSequenceObject();
					if(parentAlmt == null) {
						allColsAlmt = 
								new AllColumnsAlignment<FastaAlignmentExporter.Key>(
										new ReferenceKey(ancRef.getName()), seqObj.getNucleotides(cmdContext).length());
					} else {
						AlignmentMember refAlmtMember = GlueDataObject.lookup(cmdContext, AlignmentMember.class,
								AlignmentMember.pkMap(parentAlmt.getName(), refSeqSeq.getSource().getName(), refSeqSeq.getSequenceID()));
						allColsAlmt.addRow(
								new ReferenceKey(ancRef.getName()), 
								new ReferenceKey(parentAlmt.getRefSequence().getName()), 
								refAlmtMember.segmentsAsQueryAlignedSegments(), 
								seqObj.getNucleotides(cmdContext).length());
					}
					includedRefs.put(ancRef.getName(), ancRef);
				}
			}
		}
		// now add the alignment members themselves
		for(AlignmentMember almtMember: almtMembers) {
			Map<String,String> pkMap = almtMember.pkMap();
			AbstractSequenceObject seqObj = almtMember.getSequence().getSequenceObject();
			allColsAlmt.addRow(
					new QueryKey(pkMap), 
					new ReferenceKey(almtMember.getAlignment().getRefSequence().getName()), 
					almtMember.segmentsAsQueryAlignedSegments(), 
					seqObj.getNucleotides(cmdContext).length());
			pkMapToSeqObj.put(pkMap, seqObj);
		}
		// remove underused columns (based on usage by query sequences)
		if(minColUsage != null) {
			allColsAlmt.removeUnderusedColumns(minColUsage, k -> k instanceof QueryKey);
		}
		// set the min/max region.
		if(alignmentColumnsSelector != null && !almtMembers.isEmpty()) {
			
			// this is just a check that specified related ref is valid.
			alignment.getRelatedRef(cmdContext, alignmentColumnsSelector.getRelatedRefName());
			
			List<ReferenceSegment> featureLocRelRefSegs = alignmentColumnsSelector.selectAlignmentColumns(cmdContext);
			String relRefName = alignmentColumnsSelector.getRelatedRefName();
			List<QueryAlignedSegment> relRefToUSegs = allColsAlmt.getSegments(new ReferenceKey(relRefName));
			List<QueryAlignedSegment> uToRelRefSegs = relRefToUSegs.stream().map(seg -> seg.invert()).collect(Collectors.toList());
			List<QueryAlignedSegment> featureUToRelRefSegs = 
					ReferenceSegment.intersection(featureLocRelRefSegs, uToRelRefSegs, ReferenceSegment.cloneRightSegMerger());
			List<QueryAlignedSegment> featureRelRefToUSegs = 
					featureUToRelRefSegs.stream().map(seg -> seg.invert()).collect(Collectors.toList());
			minMaxSeg.setRefStart(ReferenceSegment.minRefStart(featureRelRefToUSegs));
			minMaxSeg.setRefEnd(ReferenceSegment.maxRefEnd(featureRelRefToUSegs));

		} else {
			for(Key key: allColsAlmt.getKeys()) {
				List<QueryAlignedSegment> qaSegs = allColsAlmt.getSegments(key);
				Integer prevRefEnd = minMaxSeg.getRefEnd();
				if(qaSegs.isEmpty()) {
					throw new CommandException(Code.COMMAND_FAILED_ERROR, "No segments in all columns alignment for key "+key);
				}
				Integer qaSegsMaxRefEnd = ReferenceSegment.maxRefEnd(qaSegs);
				int newRefEnd = Math.max(prevRefEnd, qaSegsMaxRefEnd);
				minMaxSeg.setRefEnd(newRefEnd);
			}
		}
		// finally copy the query rows of the all-columns alignment into the output map.
		for(Key key: allColsAlmt.getKeys()) {
			if(key instanceof QueryKey) {
				pkMapToQaSegs.put(((QueryKey) key).getPkMap(), allColsAlmt.getSegments(key));
			}
		}
	}
	
	private static Map<Map<String,String>, AlmtRowInfo> createAlignment(CommandContext cmdContext,
			IAlignmentColumnsSelector alignmentColumnsSelector,
			AbstractMemberSupplier memberSupplier) {
		
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
		List<ReferenceSegment> featureRefSegs = null;
		if(alignmentColumnsSelector != null) {
			featureRefSegs = alignmentColumnsSelector.selectAlignmentColumns(cmdContext);
			if(!featureRefSegs.isEmpty()) {
				minMaxSeg.setRefStart(ReferenceSegment.minRefStart(featureRefSegs));
				minMaxSeg.setRefEnd(ReferenceSegment.maxRefEnd(featureRefSegs));
			}
		}
		Map<Map<String,String>, AlmtRowInfo> pkMapToAlmtRowInfo = new LinkedHashMap<Map<String,String>, AlmtRowInfo>();
		int numMembers = memberSupplier.countMembers(cmdContext);
		GlueLogger.getGlueLogger().log(Level.INFO, "processing "+numMembers+" alignment members");
		int offset = 0;
		int processed = 0;
		int batchSize = 500;
		while(offset < numMembers) {
			Map<Map<String, String>, List<QueryAlignedSegment>> pkMapToQaSegs = 
					new LinkedHashMap<Map<String, String>, List<QueryAlignedSegment>>();
			Map<Map<String, String>, AbstractSequenceObject> pkMapToSeqObj = 
					new LinkedHashMap<Map<String, String>, AbstractSequenceObject>();
			Alignment alignment = memberSupplier.supplyAlignment(cmdContext);
			List<AlignmentMember> almtMembers = memberSupplier.supplyMembers(cmdContext, offset, batchSize);
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
			addToMemberAlignmentMap(cmdContext, minMaxSeg, pkMapToQaSegs, pkMapToSeqObj, pkMapToAlmtRowInfo);
			processed += almtMembers.size();
			GlueLogger.getGlueLogger().log(Level.INFO, "processed "+processed+" alignment members");
			offset += batchSize;
			cmdContext.newObjectContext();
		}
		return pkMapToAlmtRowInfo;
	}
	
	

	private static void addToMemberAlignmentMap(CommandContext cmdContext,
			ReferenceSegment minMaxSeg,
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
			AlmtRowInfo almtRowInfo = new AlmtRowInfo();
			almtRowInfo.dnaSequence = FastaUtils.ntStringToSequence(alignmentRow.toString());
			if(truncatedQaSegs.isEmpty()) {
				almtRowInfo.emptyRow = true;
				almtRowInfo.firstSegmentStart = maxRefNt_final;
			} else {
				almtRowInfo.emptyRow = false;
				almtRowInfo.firstSegmentStart = ReferenceSegment.minRefStart(truncatedQaSegs);
			}
			pkMapToAlmtRowInfo.put(pkMap, almtRowInfo);
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

	public static abstract class Key {}
	
	public static class ReferenceKey extends Key {
		private String refName;
		public ReferenceKey(String refName) {
			super();
			this.refName = refName;
		}
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((refName == null) ? 0 : refName.hashCode());
			return result;
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			ReferenceKey other = (ReferenceKey) obj;
			if (refName == null) {
				if (other.refName != null)
					return false;
			} else if (!refName.equals(other.refName))
				return false;
			return true;
		}
		
		public String toString() {
			return "ReferenceKey:"+refName;
		}
	}

	private static class QueryKey extends Key {
		
		private Map<String, String> pkMap;

		public QueryKey(Map<String, String> pkMap) {
			super();
			this.pkMap = pkMap;
		}

		public Map<String, String> getPkMap() {
			return pkMap;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((pkMap == null) ? 0 : pkMap.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			QueryKey other = (QueryKey) obj;
			if (pkMap == null) {
				if (other.pkMap != null)
					return false;
			} else if (!pkMap.equals(other.pkMap))
				return false;
			return true;
		}

		public String toString() {
			return "QueryKey:"+pkMap.toString();
		}
	}

	
}
