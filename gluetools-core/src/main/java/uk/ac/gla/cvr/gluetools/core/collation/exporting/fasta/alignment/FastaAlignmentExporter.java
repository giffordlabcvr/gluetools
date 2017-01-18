package uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.cayenne.exp.Expression;
import org.biojava.nbio.core.sequence.DNASequence;

import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.FastaExporterException;
import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment.FastaAlignmentExportCommandDelegate.OrderStrategy;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CommandException.Code;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.alignment.AlignmentListMemberCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.AbstractSequenceObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.segments.AllColumnsAlignment;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;
import uk.ac.gla.cvr.gluetools.core.segments.SegmentUtils;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;
import freemarker.template.Template;

@PluginClass(elemName="fastaAlignmentExporter")
public class FastaAlignmentExporter extends AbstractFastaAlignmentExporter<FastaAlignmentExporter> {
	
	public FastaAlignmentExporter() {
		super();
		addModulePluginCmdClass(FastaAlignmentExportCommand.class);
		
	}

	public CommandResult doExport(ConsoleCommandContext cmdContext, String fileName, 
			String alignmentName, Optional<Expression> whereClause, String acRefName, String featureName, 
			Boolean recursive, Boolean preview, Boolean includeAllColumns, Integer minColUsage, OrderStrategy orderStrategy) {
		String fastaAlmtString = exportAlignment(cmdContext, alignmentName,
				whereClause, acRefName, featureName, recursive, orderStrategy,
				includeAllColumns, minColUsage, getExcludeEmptyRows(), getIdTemplate());
		return formResult(cmdContext, fastaAlmtString, fileName, preview);
	}

	private static String exportAlignment(CommandContext cmdContext,
			String alignmentName, Optional<Expression> whereClause,
			String relRefName, String featureName, Boolean recursive,
			OrderStrategy orderStrategy, Boolean includeAllColumns, Integer minColUsage,
			Boolean excludeEmptyRows, Template idTemplate) {
		Alignment alignment = GlueDataObject.lookup(cmdContext, Alignment.class, Alignment.pkMap(alignmentName));
		List<AlignmentMember> almtMembers = AlignmentListMemberCommand.listMembers(cmdContext, alignment, recursive, whereClause);
		
		Map<Map<String, String>, DNASequence> memberAlignmentMap = exportAlignment(
				cmdContext, relRefName, featureName, includeAllColumns, minColUsage, excludeEmptyRows, orderStrategy,
				alignment, almtMembers);

		Map<Map<String,String>, String> pkMapToFastaId = new LinkedHashMap<Map<String,String>, String>();
		almtMembers.forEach(member -> {
			pkMapToFastaId.put(member.pkMap(), generateFastaId(idTemplate, member));
		});
		return createFastaAlignmentString(pkMapToFastaId, memberAlignmentMap);
	}

	public static Map<Map<String, String>, DNASequence> exportAlignment(
			CommandContext cmdContext, String relRefName, String featureName,
			Boolean includeAllColumns, Integer minColUsage,
			Boolean excludeEmptyRows, OrderStrategy orderStrategy, Alignment alignment,
			List<AlignmentMember> almtMembers) {
		
		ReferenceSequence refSequence = alignment.getRefSequence();
		if(refSequence == null && includeAllColumns) {
			throw new FastaExporterException(uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.FastaExporterException.Code.CANNOT_SPECIFY_INCLUDE_ALL_COLUMNS_FOR_UNCONSTRAINED_ALIGNMENT, alignment.getName());
		}

		
		ReferenceSegment minMaxSeg = new ReferenceSegment(1, 1);
		Map<Map<String, String>, List<QueryAlignedSegment>> pkMapToQaSegs = 
				new LinkedHashMap<Map<String, String>, List<QueryAlignedSegment>>();
		Map<Map<String, String>, AbstractSequenceObject> pkMapToSeqObj = 
				new LinkedHashMap<Map<String, String>, AbstractSequenceObject>();
		ReferenceSequence relatedRef = null;

		if(relRefName != null) {
			relatedRef = alignment.getRelatedRef(cmdContext, relRefName);
		}

		if(includeAllColumns) {
			createAlignmentIncludeAllColumns(cmdContext, relatedRef, featureName,
					alignment, almtMembers, minMaxSeg, pkMapToQaSegs,
					pkMapToSeqObj, minColUsage);

		} else {
			createAlignment(cmdContext, relatedRef, featureName,
					alignment, almtMembers, minMaxSeg, pkMapToQaSegs,
					pkMapToSeqObj);
		}
		Map<Map<String, String>, DNASequence> memAlmtMap = createMemberAlignmentMap(cmdContext, minMaxSeg, pkMapToQaSegs, pkMapToSeqObj);
		memAlmtMap = orderAlmt(memAlmtMap, orderStrategy);
		if(excludeEmptyRows) {
			List<Map<String,String>> keysToRemove = new ArrayList<Map<String,String>>();
			memAlmtMap.forEach((k,v) -> {
				if(v.getSequenceAsString().matches("^-*$")) {
					keysToRemove.add(k);
				}
			});
			for(Map<String,String> k: keysToRemove) {
				memAlmtMap.remove(k);
			}
		}
		
		return memAlmtMap;
	}


	private static void createAlignmentIncludeAllColumns(CommandContext cmdContext,
			ReferenceSequence acRef, String featureName,
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
		if(acRef != null && featureName != null && !almtMembers.isEmpty()) {
			FeatureLocation featureLoc = GlueDataObject.lookup(cmdContext, FeatureLocation.class,
					FeatureLocation.pkMap(acRef.getName(), featureName));
			List<ReferenceSegment> featureLocAcRefSegs = featureLoc.segmentsAsReferenceSegments();
			List<QueryAlignedSegment> acRefToUSegs = allColsAlmt.getSegments(new ReferenceKey(acRef.getName()));
			List<QueryAlignedSegment> uToAcRefSegs = acRefToUSegs.stream().map(seg -> seg.invert()).collect(Collectors.toList());
			List<QueryAlignedSegment> featureUToAcRefSegs = 
					ReferenceSegment.intersection(featureLocAcRefSegs, uToAcRefSegs, ReferenceSegment.cloneRightSegMerger());
			List<QueryAlignedSegment> featureAcRefToUSegs = 
					featureUToAcRefSegs.stream().map(seg -> seg.invert()).collect(Collectors.toList());
			minMaxSeg.setRefStart(ReferenceSegment.minRefStart(featureAcRefToUSegs));
			minMaxSeg.setRefEnd(ReferenceSegment.maxRefEnd(featureAcRefToUSegs));

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
	
	private static void createAlignment(CommandContext cmdContext,
			ReferenceSequence relatedRef, String featureName,
			Alignment alignment, List<AlignmentMember> almtMembers,
			ReferenceSegment minMaxSeg,
			Map<Map<String,String>, List<QueryAlignedSegment>> pkMapToQaSegs,
			Map<Map<String,String>, AbstractSequenceObject> pkMapToSeqObj) {
		ReferenceSequence alignmentRef = alignment.getRefSequence();
		if(alignmentRef != null) {
			minMaxSeg.setRefEnd(alignmentRef.getSequence().getSequenceObject().getNucleotides(cmdContext).length());
		} else {
			// unconstrained alignment
			for(AlignmentMember almtMember: almtMembers) {
				Integer maxRefEnd = ReferenceSegment.maxRefEnd(almtMember.getAlignedSegments());
				if(maxRefEnd != null) { minMaxSeg.setRefEnd(Math.max(maxRefEnd, minMaxSeg.getRefEnd())); }
			}
		}
		FeatureLocation featureLoc;
		List<ReferenceSegment> featureRefSegs = null;
		if(relatedRef != null && featureName != null) {
			featureLoc = GlueDataObject.lookup(cmdContext, FeatureLocation.class, FeatureLocation.pkMap(relatedRef.getName(), featureName));
			featureRefSegs = featureLoc.segmentsAsReferenceSegments();
			if(!featureRefSegs.isEmpty()) {
				minMaxSeg.setRefStart(ReferenceSegment.minRefStart(featureRefSegs));
				minMaxSeg.setRefEnd(ReferenceSegment.maxRefEnd(featureRefSegs));
			}
		}
		
		for(AlignmentMember almtMember: almtMembers) {
			Map<String,String> pkMap = almtMember.pkMap();
			List<QueryAlignedSegment> memberQaSegs = almtMember.segmentsAsQueryAlignedSegments();
			if(relatedRef != null) {
				// related reference specified in order to specify feature location
				Alignment tipAlmt = almtMember.getAlignment();
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
	}
	
	private static Map<Map<String,String>, DNASequence> createMemberAlignmentMap(CommandContext cmdContext,
			ReferenceSegment minMaxSeg,
			Map<Map<String,String>, List<QueryAlignedSegment>> pkMapToQaSegs,
			Map<Map<String,String>, AbstractSequenceObject> pkMapToSeqObj) {
		Map<Map<String,String>, DNASequence> memberAlignmentMap = new LinkedHashMap<Map<String,String>, DNASequence>();
		
		int minRefNt_final = minMaxSeg.getRefStart();
		int maxRefNt_final = minMaxSeg.getRefEnd();

		pkMapToQaSegs.forEach((pkMap, qaSegs) -> {
			
			List<QueryAlignedSegment> truncatedQaSegs = ReferenceSegment.intersection(qaSegs, Arrays.asList(minMaxSeg), ReferenceSegment.cloneLeftSegMerger());
			String memberNTs = pkMapToSeqObj.get(pkMap).getNucleotides(cmdContext);
			StringBuffer alignmentRow = new StringBuffer();
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
			memberAlignmentMap.put(pkMap, FastaUtils.ntStringToSequence(alignmentRow.toString()));
	    });
		return memberAlignmentMap;
	}

	

	private static String createFastaAlignmentString(Map<Map<String,String>, String> pkMapToFastaId,
			Map<Map<String,String>, DNASequence> memberAlignmentMap) {
		StringBuffer stringBuffer = new StringBuffer();
		memberAlignmentMap.forEach((pkMap, alignmentRow) -> {
			stringBuffer.append(FastaUtils.seqIdCompoundsPairToFasta(pkMapToFastaId.get(pkMap), alignmentRow.getSequenceAsString()));
	    });
		return stringBuffer.toString();
	}
	
	
	private static Map<Map<String,String>, DNASequence> orderAlmt(Map<Map<String,String>, DNASequence> alignment, OrderStrategy orderStrategy) {
		if(orderStrategy == null) {
			return alignment;
		}
		ArrayList<SortableAlmtMember> arrayList = 
				new ArrayList<SortableAlmtMember>(
						alignment.entrySet().stream().
						map(entry -> new SortableAlmtMember(entry)).collect(Collectors.toList()));
		Comparator<SortableAlmtMember> comparator = null;
		switch(orderStrategy) {
		case increasing_start_segment:
			comparator = new Comparator<SortableAlmtMember>() {
				@Override
				public int compare(SortableAlmtMember o1, SortableAlmtMember o2) {
					return Integer.compare(computeSortKey(o1), computeSortKey(o2));
				}
				private int computeSortKey(SortableAlmtMember mem) {
					if(mem.sortKey == null) {
						String sequenceAsString = mem.entry.getValue().getSequenceAsString();
						mem.sortKey = -1; 
						for(int i = 0; i < sequenceAsString.length(); i++) {
							if(sequenceAsString.charAt(i) == '-') {
								mem.sortKey = i;
							} else {
								break;
							}
						}
					}
					return mem.sortKey;
				}
			};
		}
		arrayList.sort(comparator);
		Map<Map<String,String>, DNASequence> sorted = new LinkedHashMap<Map<String,String>, DNASequence>();
		arrayList.forEach(mem -> {
			sorted.put(mem.entry.getKey(), mem.entry.getValue());
		});
		return sorted;
	}

	private static class SortableAlmtMember {
		Map.Entry<Map<String,String>, DNASequence> entry;
		Integer sortKey = null;
		public SortableAlmtMember(Entry<Map<String, String>, DNASequence> entry) {
			super();
			this.entry = entry;
		}
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
