package uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment.protein;

import java.util.List;
import java.util.logging.Level;

import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment.AbstractAlmtRowConsumer;
import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment.AbstractFastaAlignmentExporter;
import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment.IAlignmentColumnsSelector;
import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.memberSupplier.AbstractMemberSupplier;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.AbstractSequenceObject;
import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;
import uk.ac.gla.cvr.gluetools.core.segments.SegmentUtils;
import uk.ac.gla.cvr.gluetools.core.translation.CommandContextTranslator;
import uk.ac.gla.cvr.gluetools.core.translation.TranslationUtils;
import uk.ac.gla.cvr.gluetools.core.translation.Translator;

@PluginClass(elemName="fastaProteinAlignmentExporter")
public class FastaProteinAlignmentExporter extends AbstractFastaAlignmentExporter<FastaProteinAlignmentExporter> {
	
	public FastaProteinAlignmentExporter() {
		super();
		addModulePluginCmdClass(FastaProteinAlignmentExportCommand.class);
		addModulePluginCmdClass(FastaProteinAlignmentWebExportCommand.class);
	}

	public static void exportAlignment(
			CommandContext cmdContext, String featureName, IAlignmentColumnsSelector alignmentColumnsSelector, Boolean excludeEmptyRows,
			AbstractMemberSupplier memberSupplier, AbstractAlmtRowConsumer almtRowConsumer) {

		List<ReferenceSegment> featureRefSegs = alignmentColumnsSelector.selectAlignmentColumns(cmdContext);
		ReferenceSegment minMaxSeg = initMinMaxSeg(featureRefSegs);

		// quick check that feature codes AAs
		String relatedRefName = alignmentColumnsSelector.getRelatedRefName();
		FeatureLocation featureLoc = GlueDataObject.lookup(cmdContext, FeatureLocation.class, FeatureLocation.pkMap(relatedRefName, featureName));
		featureLoc.getFeature().checkCodesAminoAcids();
		
		int numMembers = memberSupplier.countMembers(cmdContext);
		GlueLogger.getGlueLogger().log(Level.INFO, "processing "+numMembers+" alignment members");
		int offset = 0;
		int processed = 0;
		int batchSize = 500;
		while(offset < numMembers) {
			Alignment alignment = memberSupplier.supplyAlignment(cmdContext);
			List<AlignmentMember> almtMembers = memberSupplier.supplyMembers(cmdContext, offset, batchSize);
			createAlignment(cmdContext, excludeEmptyRows, featureName, alignmentColumnsSelector, alignment, almtMembers, featureRefSegs, minMaxSeg, almtRowConsumer);
			processed += almtMembers.size();
			GlueLogger.getGlueLogger().log(Level.INFO, "processed "+processed+" alignment members");
			offset += batchSize;
			cmdContext.newObjectContext();
		}
	}

	private static void createAlignment(CommandContext cmdContext,
			Boolean excludeEmptyRows,
			String featureName, 
			IAlignmentColumnsSelector alignmentColumnsSelector,
			Alignment alignment, List<AlignmentMember> almtMembers,
			List<ReferenceSegment> featureRefSegs, ReferenceSegment minMaxSeg,
			AbstractAlmtRowConsumer almtRowConsumer) {
		String relatedRefName = alignmentColumnsSelector.getRelatedRefName();
		ReferenceSequence relatedRef = alignment.getAncConstrainingRef(cmdContext, relatedRefName);
		FeatureLocation featureLoc = GlueDataObject.lookup(cmdContext, FeatureLocation.class, FeatureLocation.pkMap(relatedRefName, featureName));

		int codon1Start = featureLoc.getCodon1Start(cmdContext);
		Translator translator = new CommandContextTranslator(cmdContext);
		for(AlignmentMember almtMember: almtMembers) {
			List<QueryAlignedSegment> memberQaSegs = almtMember.segmentsAsQueryAlignedSegments();
			Alignment tipAlmt = almtMember.getAlignment();
			memberQaSegs = tipAlmt.translateToAncConstrainingRef(cmdContext, memberQaSegs, relatedRef);
			memberQaSegs = ReferenceSegment.intersection(memberQaSegs, featureRefSegs, ReferenceSegment.cloneLeftSegMerger());
			
			// important to merge abutting here otherwise you may get gaps if the boundary is within a codon.
			memberQaSegs = QueryAlignedSegment.mergeAbutting(memberQaSegs, 
					QueryAlignedSegment.mergeAbuttingFunctionQueryAlignedSegment(), 
					QueryAlignedSegment.abutsPredicateQueryAlignedSegment());

			
			memberQaSegs = TranslationUtils.truncateToCodonAligned(codon1Start, memberQaSegs);
			if(excludeEmptyRows && memberQaSegs.isEmpty()) {
				continue;
			}

			AbstractSequenceObject seqObj = almtMember.getSequence().getSequenceObject();
			String memberNTs = seqObj.getNucleotides(cmdContext);
			StringBuffer alignmentRow = new StringBuffer();
			int ntIndex = minMaxSeg.getRefStart();
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
			while(ntIndex <= minMaxSeg.getRefEnd()) {
				alignmentRow.append("-");
				ntIndex += 3;
			}
			almtRowConsumer.consumeAlmtRow(cmdContext, almtMember, alignmentRow.toString());
		}
	}

	private static ReferenceSegment initMinMaxSeg(
			List<ReferenceSegment> featureRefSegs) {
		int minRefNt = 1;
		int maxRefNt = 1;
		if(!featureRefSegs.isEmpty()) {
			minRefNt = ReferenceSegment.minRefStart(featureRefSegs);
			maxRefNt = ReferenceSegment.maxRefEnd(featureRefSegs);
		}
		ReferenceSegment minMaxSeg = new ReferenceSegment(minRefNt, maxRefNt);
		return minMaxSeg;
	}

}
