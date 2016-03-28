package uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment;

import java.util.List;
import java.util.Optional;

import org.apache.cayenne.exp.Expression;

import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.alignment.AlignmentListMemberCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.AbstractSequenceObject;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
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
			Boolean recursive, Boolean preview) {
		Alignment alignment = GlueDataObject.lookup(cmdContext, Alignment.class, Alignment.pkMap(alignmentName));
		checkAlignment(alignment, featureName, recursive);
		ReferenceSequence refSequence = alignment.getRefSequence();
		List<AlignmentMember> almtMembers = AlignmentListMemberCommand.listMembers(cmdContext, alignment, recursive, getDeduplicate(), whereClause);
		
		int minRefNt = 1;
		int maxRefNt = 1;
		if(refSequence != null) {
			maxRefNt = refSequence.getSequence().getSequenceObject().getNucleotides(cmdContext).length();
		} else {
			// unconstrained alignment
			for(AlignmentMember almtMember: almtMembers) {
				Integer maxRefEnd = ReferenceSegment.maxRefEnd(almtMember.getAlignedSegments());
				if(maxRefEnd != null) { maxRefNt = Math.max(maxRefEnd, maxRefNt); }
			}
		}
		ReferenceSequence acRef = null;
		FeatureLocation featureLoc;
		List<ReferenceSegment> featureRefSegs = null;
		if(acRefName != null) {
			acRef = alignment.getAncConstrainingRef(cmdContext, acRefName);
			if(featureName != null) {
				featureLoc = GlueDataObject.lookup(cmdContext, FeatureLocation.class, FeatureLocation.pkMap(acRefName, featureName));
				featureRefSegs = featureLoc.segmentsAsReferenceSegments();
				if(!featureRefSegs.isEmpty()) {
					minRefNt = ReferenceSegment.minRefStart(featureRefSegs);
					maxRefNt = ReferenceSegment.maxRefEnd(featureRefSegs);
				}
			}
		}

		StringBuffer stringBuffer = new StringBuffer();
		
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
			AbstractSequenceObject seqObj = almtMember.getSequence().getSequenceObject();
			String memberNTs = seqObj.getNucleotides(cmdContext);
			StringBuffer alignmentRow = new StringBuffer();
			int ntIndex = minRefNt;
			for(QueryAlignedSegment seg: memberQaSegs) {
				while(ntIndex < seg.getRefStart()) {
					alignmentRow.append("-");
					ntIndex++;
				}
				alignmentRow.append(SegmentUtils.base1SubString(memberNTs, seg.getQueryStart(), seg.getQueryEnd()));
				ntIndex = seg.getRefEnd()+1;
			}
			while(ntIndex <= maxRefNt) {
				alignmentRow.append("-");
				ntIndex++;
			}
			stringBuffer.append(FastaUtils.seqIdCompoundsPairToFasta(fastaId, alignmentRow.toString()));
		}
		String fastaString = stringBuffer.toString();
		return formResult(cmdContext, fastaString, fileName, preview);
	}
	
}
