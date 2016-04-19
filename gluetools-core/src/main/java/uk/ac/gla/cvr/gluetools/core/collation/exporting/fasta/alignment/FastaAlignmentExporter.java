package uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.cayenne.exp.Expression;

import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.FastaExporterException;
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

@PluginClass(elemName="fastaAlignmentExporter")
public class FastaAlignmentExporter extends AbstractFastaAlignmentExporter<FastaAlignmentExporter> {
	
	public FastaAlignmentExporter() {
		super();
		addModulePluginCmdClass(FastaAlignmentExportCommand.class);
	}

	public CommandResult doExport(ConsoleCommandContext cmdContext, String fileName, 
			String alignmentName, Optional<Expression> whereClause, String acRefName, String featureName, 
			Boolean recursive, Boolean preview, Boolean includeAllColumns) {
		Alignment alignment = GlueDataObject.lookup(cmdContext, Alignment.class, Alignment.pkMap(alignmentName));
		checkAlignment(alignment, featureName, recursive);
		List<AlignmentMember> almtMembers = AlignmentListMemberCommand.listMembers(cmdContext, alignment, recursive, getDeduplicate(), whereClause);
		
		ReferenceSegment minMaxSeg = new ReferenceSegment(1, 1);
		Map<String, List<QueryAlignedSegment>> idToQaSegs = new LinkedHashMap<String, List<QueryAlignedSegment>>();
		Map<String, AbstractSequenceObject> idToSeqObj = new LinkedHashMap<String, AbstractSequenceObject>();
		ReferenceSequence acRef = null;

		if(acRefName != null) {
			acRef = alignment.getAncConstrainingRef(cmdContext, acRefName);
		}

		if(includeAllColumns) {
			createAlignmentIncludeAllColumns(cmdContext, acRef, featureName,
					alignment, almtMembers, minMaxSeg, idToQaSegs,
					idToSeqObj);

		} else {
			createAlignment(cmdContext, acRef, featureName,
					alignment, almtMembers, minMaxSeg, idToQaSegs,
					idToSeqObj);
		}


		String fastaAlmtString = createFastaAlignmentString(cmdContext, minMaxSeg, idToQaSegs, idToSeqObj);
		return formResult(cmdContext, fastaAlmtString, fileName, preview);
	}


	private void createAlignmentIncludeAllColumns(ConsoleCommandContext cmdContext,
			ReferenceSequence acRef, String featureName,
			Alignment alignment, List<AlignmentMember> almtMembers,
			ReferenceSegment minMaxSeg,
			Map<String, List<QueryAlignedSegment>> idToQaSegs,
			Map<String, AbstractSequenceObject> idToSeqObj) {
		
		AllColumnsAlignment<Key> allColsAlmt = null;
		
		ReferenceSequence refSequence = alignment.getRefSequence();

		if(refSequence != null) {
			// ensure all necessary references are in the alignment (we will delete them later).
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
				String fastaID = generateFastaId(almtMember);
				AbstractSequenceObject seqObj = almtMember.getSequence().getSequenceObject();
				allColsAlmt.addRow(
						new QueryKey(fastaID), 
						new ReferenceKey(almtMember.getAlignment().getRefSequence().getName()), 
						almtMember.segmentsAsQueryAlignedSegments(), 
						seqObj.getNucleotides(cmdContext).length());
				idToSeqObj.put(fastaID, seqObj);
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
					minMaxSeg.setRefEnd(Math.max(minMaxSeg.getRefEnd(), ReferenceSegment.maxRefEnd(qaSegs)));
				}
			}
			// finally copy the query rows of the all-columns alignment into the output map.
			for(Key key: allColsAlmt.getKeys()) {
				if(key instanceof QueryKey) {
					idToQaSegs.put(((QueryKey) key).getFastaID(), allColsAlmt.getSegments(key));
				}
			}
		} else {
			throw new FastaExporterException(FastaExporterException.Code.INCLUDE_ALL_COLUMNS_UNIMPLEMENTED_FOR_UNCONSTRAINED_ALIGNMENT, alignment.getName());
		}
	}
	
	private void createAlignment(ConsoleCommandContext cmdContext,
			ReferenceSequence acRef, String featureName,
			Alignment alignment, List<AlignmentMember> almtMembers,
			ReferenceSegment minMaxSeg,
			Map<String, List<QueryAlignedSegment>> idToQaSegs,
			Map<String, AbstractSequenceObject> idToSeqObj) {
		ReferenceSequence refSequence = alignment.getRefSequence();
		if(refSequence != null) {
			minMaxSeg.setRefEnd(refSequence.getSequence().getSequenceObject().getNucleotides(cmdContext).length());
		} else {
			// unconstrained alignment
			for(AlignmentMember almtMember: almtMembers) {
				Integer maxRefEnd = ReferenceSegment.maxRefEnd(almtMember.getAlignedSegments());
				if(maxRefEnd != null) { minMaxSeg.setRefEnd(Math.max(maxRefEnd, minMaxSeg.getRefEnd())); }
			}
		}
		FeatureLocation featureLoc;
		List<ReferenceSegment> featureRefSegs = null;
		if(acRef != null && featureName != null) {
			featureLoc = GlueDataObject.lookup(cmdContext, FeatureLocation.class, FeatureLocation.pkMap(acRef.getName(), featureName));
			featureRefSegs = featureLoc.segmentsAsReferenceSegments();
			if(!featureRefSegs.isEmpty()) {
				minMaxSeg.setRefStart(ReferenceSegment.minRefStart(featureRefSegs));
				minMaxSeg.setRefEnd(ReferenceSegment.maxRefEnd(featureRefSegs));
			}
		}
		
		for(AlignmentMember almtMember: almtMembers) {
			String fastaId = generateFastaId(almtMember);
			List<QueryAlignedSegment> memberQaSegs = almtMember.segmentsAsQueryAlignedSegments();
			if(acRef != null) {
				Alignment tipAlmt = almtMember.getAlignment();
				memberQaSegs = tipAlmt.translateToAncConstrainingRef(cmdContext, memberQaSegs, acRef);
			}
			if(featureRefSegs != null) {
				memberQaSegs = ReferenceSegment.intersection(memberQaSegs, featureRefSegs, ReferenceSegment.cloneLeftSegMerger());
			}
			idToQaSegs.put(fastaId, memberQaSegs);
			idToSeqObj.put(fastaId, almtMember.getSequence().getSequenceObject());
		}
	}

	private String createFastaAlignmentString(ConsoleCommandContext cmdContext,
			ReferenceSegment minMaxSeg,
			Map<String, List<QueryAlignedSegment>> idToQaSegs,
			Map<String, AbstractSequenceObject> idToSeqObj) {
		StringBuffer stringBuffer = new StringBuffer();
		int minRefNt_final = minMaxSeg.getRefStart();
		int maxRefNt_final = minMaxSeg.getRefEnd();

		idToQaSegs.forEach((fastaId, qaSegs) -> {
			
			List<QueryAlignedSegment> truncatedQaSegs = ReferenceSegment.intersection(qaSegs, Arrays.asList(minMaxSeg), ReferenceSegment.cloneLeftSegMerger());
			String memberNTs = idToSeqObj.get(fastaId).getNucleotides(cmdContext);
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
			stringBuffer.append(FastaUtils.seqIdCompoundsPairToFasta(fastaId, alignmentRow.toString()));
	    });
		String fastaString = stringBuffer.toString();
		return fastaString;
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
	}

	private static class QueryKey extends Key {
		private String fastaID;

		public QueryKey(String fastaID) {
			super();
			this.fastaID = fastaID;
		}

		public String getFastaID() {
			return fastaID;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((fastaID == null) ? 0 : fastaID.hashCode());
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
			if (fastaID == null) {
				if (other.fastaID != null)
					return false;
			} else if (!fastaID.equals(other.fastaID))
				return false;
			return true;
		}
		
	}

	
}
