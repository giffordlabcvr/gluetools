package uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment.protein;

import java.util.List;
import java.util.Optional;

import org.apache.cayenne.exp.Expression;

import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment.AbstractFastaAlignmentExporter;
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
import uk.ac.gla.cvr.gluetools.core.translation.CommandContextTranslator;
import uk.ac.gla.cvr.gluetools.core.translation.TranslationUtils;
import uk.ac.gla.cvr.gluetools.core.translation.Translator;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;

@PluginClass(elemName="fastaProteinAlignmentExporter")
public class FastaProteinAlignmentExporter extends AbstractFastaAlignmentExporter<FastaProteinAlignmentExporter> {
	
	public FastaProteinAlignmentExporter() {
		super();
		addModulePluginCmdClass(FastaProteinAlignmentExportCommand.class);
	}

	public CommandResult doExport(ConsoleCommandContext cmdContext, String fileName, 
			String alignmentName, Optional<Expression> whereClause, String acRefName, String featureName, 
			Boolean recursive, Boolean preview) {
		Alignment alignment = GlueDataObject.lookup(cmdContext, Alignment.class, Alignment.pkMap(alignmentName));
		List<AlignmentMember> almtMembers = AlignmentListMemberCommand.listMembers(cmdContext, alignment, recursive, whereClause);
		
		int minRefNt = 1;
		int maxRefNt = 1;
		ReferenceSequence acRef = null;
		FeatureLocation featureLoc;
		List<ReferenceSegment> featureRefSegs = null;
		acRef = alignment.getAncConstrainingRef(cmdContext, acRefName);
		featureLoc = GlueDataObject.lookup(cmdContext, FeatureLocation.class, FeatureLocation.pkMap(acRefName, featureName));
		featureLoc.getFeature().checkCodesAminoAcids();
		
		int codon1Start = featureLoc.getCodon1Start(cmdContext);
		
		featureRefSegs = featureLoc.segmentsAsReferenceSegments();
		if(!featureRefSegs.isEmpty()) {
			minRefNt = ReferenceSegment.minRefStart(featureRefSegs);
			maxRefNt = ReferenceSegment.maxRefEnd(featureRefSegs);
		}

		StringBuffer stringBuffer = new StringBuffer();
		Translator translator = new CommandContextTranslator(cmdContext);
		
		for(AlignmentMember almtMember: almtMembers) {
			String fastaId = generateFastaId(almtMember);
			List<QueryAlignedSegment> memberQaSegs = almtMember.segmentsAsQueryAlignedSegments();
			Alignment tipAlmt = almtMember.getAlignment();
			memberQaSegs = tipAlmt.translateToAncConstrainingRef(cmdContext, memberQaSegs, acRef);
			memberQaSegs = ReferenceSegment.intersection(memberQaSegs, featureRefSegs, ReferenceSegment.cloneLeftSegMerger());
			memberQaSegs = TranslationUtils.truncateToCodonAligned(codon1Start, memberQaSegs);
			
			AbstractSequenceObject seqObj = almtMember.getSequence().getSequenceObject();
			String memberNTs = seqObj.getNucleotides(cmdContext);
			StringBuffer alignmentRow = new StringBuffer();
			int ntIndex = minRefNt;
			for(QueryAlignedSegment seg: memberQaSegs) {
				while(ntIndex < seg.getRefStart()) {
					alignmentRow.append("-");
					ntIndex += 3;
				}
				String segNTs = SegmentUtils.base1SubString(memberNTs, seg.getQueryStart(), seg.getQueryEnd());
				String segAAs = translator.translate(segNTs);
				alignmentRow.append(segAAs);
				ntIndex = seg.getRefEnd()+1;
			}
			while(ntIndex <= maxRefNt) {
				alignmentRow.append("-");
				ntIndex += 3;
			}
			stringBuffer.append(FastaUtils.seqIdCompoundsPairToFasta(fastaId, alignmentRow.toString()));
		}
		String fastaString = stringBuffer.toString();
		return formResult(cmdContext, fastaString, fileName, preview);
	}

}
