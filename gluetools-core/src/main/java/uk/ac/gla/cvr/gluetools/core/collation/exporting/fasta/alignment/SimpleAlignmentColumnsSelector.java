package uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment;

import java.util.List;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.reporting.alignmentColumnSelector.AminoAcidRegionSelector;
import uk.ac.gla.cvr.gluetools.core.reporting.alignmentColumnSelector.NucleotideRegionSelector;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;

public class SimpleAlignmentColumnsSelector implements IAlignmentColumnsSelector {

	private String relatedRefName;
	private String featureName;
	
	private Integer ntStart;
	private Integer ntEnd;
	private String lcStart;
	private String lcEnd;
	
	public SimpleAlignmentColumnsSelector(String relatedRefName,
			String featureName, Integer ntStart, Integer ntEnd, String lcStart,
			String lcEnd) {
		super();
		this.relatedRefName = relatedRefName;
		this.featureName = featureName;
		this.ntStart = ntStart;
		this.ntEnd = ntEnd;
		this.lcStart = lcStart;
		this.lcEnd = lcEnd;
	}

	@Override
	public List<ReferenceSegment> selectAlignmentColumns(CommandContext cmdContext) {
		if(lcStart == null && lcEnd == null && ntStart == null && ntEnd == null) {
			FeatureLocation featureLoc = GlueDataObject.lookup(cmdContext, FeatureLocation.class, FeatureLocation.pkMap(relatedRefName, featureName));
			List<ReferenceSegment> featureRefSegs = featureLoc.segmentsAsReferenceSegments();
			return featureRefSegs;
		}
		if((lcStart != null || lcEnd != null) && (ntStart != null || ntEnd != null)) {
			throw new RuntimeException("Cannot specify both labelledCodon and ntRegion");
		}
		if(lcStart != null || lcEnd != null) {
			return AminoAcidRegionSelector.selectAlignmentColumns(cmdContext, relatedRefName, featureName, lcStart, lcEnd);
		}
		if(ntStart != null || ntEnd != null) {
			return NucleotideRegionSelector.selectAlignmentColumns(cmdContext, relatedRefName, featureName, ntStart, ntEnd);
		}
		throw new RuntimeException("Badly specified labelledCodon / ntRegion");
	}

	@Override
	public String getRelatedRefName() {
		return relatedRefName;
	}

	public String getFeatureName() {
		return featureName;
	}
	
	
	
}
