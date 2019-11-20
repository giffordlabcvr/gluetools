package uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CommandException.Code;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureSegment.FeatureSegment;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;

public class InsertionSpanningAlignmentColumnsSelector implements IAlignmentColumnsSelector {

	private IAlignmentColumnsSelector delegate;

	public InsertionSpanningAlignmentColumnsSelector(IAlignmentColumnsSelector delegate) {
		super();
		this.delegate = delegate;
	}

	public List<FeatureReferenceSegment> selectAlignmentColumns(Alignment alignment, CommandContext cmdContext) {
		if(alignment.isConstrained()) {
			throw new CommandException(Code.COMMAND_FAILED_ERROR, "Cannot include insertions: alignment is constrained");
		}
		String relatedRefName = delegate.getRelatedRefName();

		// find mapping from ref to alignment's native (u) coordinates
		ReferenceSequence relatedRef = GlueDataObject.lookup(cmdContext, ReferenceSequence.class, ReferenceSequence.pkMap(relatedRefName));
		AlignmentMember relatedRefMember = GlueDataObject.lookup(cmdContext, AlignmentMember.class, 
				AlignmentMember.pkMap(alignment.getName(), relatedRef.getSequence().getSource().getName(), relatedRef.getSequence().getSequenceID()));
		List<QueryAlignedSegment> relRefToUSegs = relatedRefMember.segmentsAsQueryAlignedSegments();
		
		// select alignment columns
		List<FeatureReferenceSegment> featureRefSegs = delegate.selectAlignmentColumns(alignment, cmdContext);
		
		// group selected alignment columns by feature
		Map<Object, List<FeatureReferenceSegment>> featureNameToRefSegs = featureRefSegs.stream().collect(Collectors.groupingBy(frs -> frs.getFeatureName()));
		
		List<FeatureReferenceSegment> results = new ArrayList<FeatureReferenceSegment>();
		featureNameToRefSegs.forEach( ( featureNameObj, frSegs ) -> {
			// for a given feature
			String featureName = (String) featureNameObj;
			FeatureLocation fLoc = GlueDataObject.lookup(cmdContext, FeatureLocation.class, FeatureLocation.pkMap(relatedRefName, featureName), false);
			List<FeatureSegment> featureSegments = fLoc.getSegments();
			
			// intersect the selected FR segments with the featureSegments to get the splice index on each FR seg.
			List<SpliceIndexFeatureRefSeg> spliceFeatureRefSegs = ReferenceSegment.intersection(featureSegments, frSegs, 
					new BiFunction<FeatureSegment, FeatureReferenceSegment, SpliceIndexFeatureRefSeg>() {
						@Override
						public SpliceIndexFeatureRefSeg apply(FeatureSegment fs, FeatureReferenceSegment frs) {
							FeatureReferenceSegment mergedFrs = (FeatureReferenceSegment) ReferenceSegment.cloneRightSegMerger().apply(fs, frs);
							return new SpliceIndexFeatureRefSeg(mergedFrs.getFeatureName(), fs.getSpliceIndex(), mergedFrs.getRefStart(), mergedFrs.getRefEnd());
						}
			});
			
			// group by splice index.
			Map<Integer, List<SpliceIndexFeatureRefSeg>> spliceIndexToSegs = spliceFeatureRefSegs.stream().collect(Collectors.groupingBy(sifrs -> sifrs.spliceIndex));
			
			// for each set with the same splice index
			spliceIndexToSegs.forEach( (spliceIndex, siSegs) -> {
				// map it into u coordinates
				List<QueryAlignedSegment> siQaSegs = siSegs.stream()
						.map(sis -> new QueryAlignedSegment(sis.getRefStart(), sis.getRefEnd(), sis.getRefStart(), sis.getRefEnd()))
						.collect(Collectors.toList());
				
				List<QueryAlignedSegment> siRefToUSegs = QueryAlignedSegment.translateSegments(siQaSegs, relRefToUSegs);
				// spanning happens now
				results.add(new FeatureReferenceSegment(featureName, ReferenceSegment.minRefStart(siRefToUSegs), ReferenceSegment.maxRefEnd(siRefToUSegs)));
			} );
		});
		
		return results;
	}

	public String getRelatedRefName() {
		return null;
	}

	public void checkAminoAcidSelector(CommandContext cmdContext) {
		delegate.checkAminoAcidSelector(cmdContext);
	}
		
	private static class SpliceIndexFeatureRefSeg extends FeatureReferenceSegment {
		
		int spliceIndex;

		public SpliceIndexFeatureRefSeg(String featureName, int spliceIndex, int refStart, int refEnd) {
			super(featureName, refStart, refEnd);
			this.spliceIndex = spliceIndex;
		}
		
	}
	
}