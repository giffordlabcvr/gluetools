package uk.ac.gla.cvr.gluetools.core.gbSubmissionGenerator.featureProvider;

import java.util.List;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.feature.Feature;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.gbSubmissionGenerator.featureProvider.FeatureProviderException.Code;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;

// FeatureProvider which generates a feature in a GenBank submission, based on the sequence's membership of a 
// an unconstrained alignment, and the homology between the sequence and a named reference within that alignment.

// There must be a feature location on the named reference of this alignment which has the feature
// named in the <glueFeatureName> element. The homology of the sequence within the unconstrained alignment, 
// in the region of this feature location, is used to generate the genbank feature specification.

@PluginClass(elemName="unconstrainedAlignmentFeatureProvider")
public class UnconstrainedAlignmentFeatureProvider extends AlignmentFeatureProvider {

	public static final String ALIGNMENT_NAME = "alignmentName";

	public static final String REFERENCE_SEQUENCE_NAME = "referenceSequenceName";
	
	private String alignmentName;
	private String referenceSequenceName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.alignmentName = PluginUtils.configureStringProperty(configElem, ALIGNMENT_NAME, true);
		this.referenceSequenceName = PluginUtils.configureStringProperty(configElem, REFERENCE_SEQUENCE_NAME, true);
	}

	@Override
	public GbFeatureSpecification provideFeature(CommandContext cmdContext, Sequence sequence) {
		AlignmentMember seqAlmtMember = GlueDataObject.lookup(cmdContext, AlignmentMember.class,
				AlignmentMember.pkMap(alignmentName, sequence.getSource().getName(), sequence.getSequenceID()));

		Alignment alignment = seqAlmtMember.getAlignment();
		if(alignment.isConstrained()) {
			throw new FeatureProviderException(Code.CONFIG_ERROR, "Alignment "+alignment.getName()+" is constrained");
		}

		ReferenceSequence refSequence = 
				GlueDataObject.lookup(cmdContext, ReferenceSequence.class, Feature.pkMap(referenceSequenceName));

		List<QueryAlignedSegment> queryToAlmtSegs = seqAlmtMember.segmentsAsQueryAlignedSegments();
		List<QueryAlignedSegment> allMemberToRefSegments = 
				alignment.translateToRelatedRef(cmdContext, queryToAlmtSegs, refSequence);

		return generateGbFeatureSpecification(cmdContext, sequence, refSequence, allMemberToRefSegments, false);
	}

	

	
}
