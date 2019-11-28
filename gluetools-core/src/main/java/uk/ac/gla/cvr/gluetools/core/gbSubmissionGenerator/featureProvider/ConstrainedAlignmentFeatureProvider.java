package uk.ac.gla.cvr.gluetools.core.gbSubmissionGenerator.featureProvider;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.gbSubmissionGenerator.featureProvider.FeatureProviderException.Code;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;

// FeatureProvider which generates a feature in a GenBank submission, based on the Sequence's membership of a 
// constrained alignment. 

// There must be a feature location on the constraining reference of this alignment which has the feature
// named in the <glueFeatureName> element. The homology of the sequence within the constrained alignment, 
// in the region of this feature location, is used to generate the genbank feature specification.



@PluginClass(elemName="constrainedAlignmentFeatureProvider")
public class ConstrainedAlignmentFeatureProvider extends AlignmentFeatureProvider {

	// An ancestor alignment is specified, the constrained alignment we use may be this alignment
	// or any of its descendents. 
	public static final String ANCESTOR_ALIGNMENT_NAME = "ancestorAlignmentName";

	// default: false. If true, the feature location may be absent on the selected constraining reference,
	// in this case no GenBank feature will be provided.
	public static final String FEATURE_LOCATION_MAY_BE_ABSENT = "featureLocationMayBeAbsent";

	// default: false. If true, the alignment member may be absent,
	// in this case no GenBank feature will be provided.
	public static final String ALIGNMENT_MEMBER_MAY_BE_ABSENT = "alignmentMemberMayBeAbsent";
	
	private String ancestorAlignmentName;
	private boolean featureLocationMayBeAbsent;
	private Boolean alignmentMemberMayBeAbsent;

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.ancestorAlignmentName = PluginUtils.configureStringProperty(configElem, ANCESTOR_ALIGNMENT_NAME, true);
		this.featureLocationMayBeAbsent = Optional.ofNullable(PluginUtils
				.configureBooleanProperty(configElem, FEATURE_LOCATION_MAY_BE_ABSENT, false)).orElse(false);
		this.alignmentMemberMayBeAbsent = Optional.ofNullable(PluginUtils
				.configureBooleanProperty(configElem, ALIGNMENT_MEMBER_MAY_BE_ABSENT, false)).orElse(false);
	}

	@Override
	public GbFeatureSpecification provideFeature(CommandContext cmdContext, Sequence sequence) {
		Alignment ancestorAlmt = GlueDataObject.lookup(cmdContext, Alignment.class, Alignment.pkMap(ancestorAlignmentName));
		if(!ancestorAlmt.isConstrained()) {
			throw new FeatureProviderException(Code.CONFIG_ERROR, "Alignment "+ancestorAlmt.getName()+" is not constrained");
		}
		AlignmentMember almtMember = getUniqueAlignmentMembership(sequence, ancestorAlmt);
		
		if(almtMember == null) {
			return null;
		}
		ReferenceSequence constrainingReference = almtMember.getAlignment().getRefSequence();

		List<QueryAlignedSegment> allMemberToRefSegments = almtMember.segmentsAsQueryAlignedSegments();

		return generateGbFeatureSpecification(cmdContext, sequence,
				constrainingReference, allMemberToRefSegments, featureLocationMayBeAbsent);
	}

	// If the sequence is a member of multiple alignment nodes in the given tree, a unique constrained alignment node 
	// which is a descendent of all the other nodes it is a member of is searched for. If no such node is found, an 
	// exception is thrown. 
	private AlignmentMember getUniqueAlignmentMembership(Sequence sequence, Alignment ancestorAlmt) {
		List<AlignmentMember> constrainedAlignmentMemberships = getConstrainedAlignmentMemberships(sequence, ancestorAlmt);
		if(constrainedAlignmentMemberships.size() == 0) {
			if(this.alignmentMemberMayBeAbsent) {
				return null;
			}
			throw new FeatureProviderException(Code.UNABLE_TO_ESTABLISH_ALIGNMENT_MEMBER, sequence.getSource().getName(), sequence.getSequenceID(), 
					"Not a member of constrained alignment "+ancestorAlmt.getName()+" or one of its descendents");
		}
		if(constrainedAlignmentMemberships.size() > 1) {
			// amongst multiple memberships, it's not a problem if there is just one which is the descendent of
			// all the others. See if this is the case.
			for(AlignmentMember almtMember: constrainedAlignmentMemberships) {
				Set<Alignment> ancestors = new LinkedHashSet<Alignment>(almtMember.getAlignment().getAncestors());
				ancestors.remove(almtMember.getAlignment());
				boolean allOtherMembersAreInAncestors = true;
				List<AlignmentMember> otherMembers = new ArrayList<AlignmentMember>(constrainedAlignmentMemberships);
				otherMembers.remove(almtMember);
				for(AlignmentMember otherMember: otherMembers) {
					if(ancestors.contains(otherMember.getAlignment())) {
						continue;
					}
					allOtherMembersAreInAncestors = false;
					break;
				}
				if(allOtherMembersAreInAncestors) {
					return almtMember;
				}
			}
			throw new FeatureProviderException(Code.UNABLE_TO_ESTABLISH_ALIGNMENT_MEMBER, sequence.getSource().getName(), sequence.getSequenceID(), 
					"It is a member of multiple constrained alignments which are equally-distant descendents of "+ancestorAlmt.getName());
		}
		return constrainedAlignmentMemberships.get(0);
	}

	private List<AlignmentMember> getConstrainedAlignmentMemberships(Sequence sequence, Alignment ancestorAlmt) {
		String ancestorName = ancestorAlmt.getName();
		return sequence.getAlignmentMemberships()
				.stream()
				.filter(am -> am.getAlignment().isConstrained())
				.filter(am -> am.getAlignment().getAncestorNames().contains(ancestorName))
				.collect(Collectors.toList());
	}
	
	
}
