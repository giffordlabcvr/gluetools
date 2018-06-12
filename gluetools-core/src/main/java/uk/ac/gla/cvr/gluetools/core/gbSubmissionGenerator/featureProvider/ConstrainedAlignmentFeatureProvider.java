package uk.ac.gla.cvr.gluetools.core.gbSubmissionGenerator.featureProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.feature.Feature;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.gbSubmissionGenerator.featureProvider.FeatureProviderException.Code;
import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.segments.IReferenceSegment;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;
import uk.ac.gla.cvr.gluetools.core.translation.TranslationUtils;

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
	
	private String ancestorAlignmentName;
	private boolean featureLocationMayBeAbsent;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.ancestorAlignmentName = PluginUtils.configureStringProperty(configElem, ANCESTOR_ALIGNMENT_NAME, true);
		this.featureLocationMayBeAbsent = Optional.ofNullable(PluginUtils
				.configureBooleanProperty(configElem, FEATURE_LOCATION_MAY_BE_ABSENT, false)).orElse(false);
	}

	@Override
	public GbFeatureSpecification provideFeature(CommandContext cmdContext, Sequence sequence) {
		Alignment ancestorAlmt = GlueDataObject.lookup(cmdContext, Alignment.class, Alignment.pkMap(ancestorAlignmentName));
		if(!ancestorAlmt.isConstrained()) {
			throw new FeatureProviderException(Code.CONFIG_ERROR, "Alignment "+ancestorAlmt.getName()+" is not constrained");
		}
		Feature feature = GlueDataObject.lookup(cmdContext, Feature.class, Feature.pkMap(getGlueFeatureName()));
		AlignmentMember almtMember = getUniqueAlignmentMembership(sequence, ancestorAlmt);
		ReferenceSequence constrainingReference = almtMember.getAlignment().getRefSequence();

		FeatureLocation featureLocation = 
				GlueDataObject.lookup(cmdContext, FeatureLocation.class, 
						FeatureLocation.pkMap(constrainingReference.getName(), feature.getName()), false);

		if(featureLocation == null) {
			if(featureLocationMayBeAbsent) {
				GlueLogger.log(Level.FINEST, "No GenBank feature generated for sequence "+
						sequence.getSource().getName()+"/"+sequence.getSequenceID()+", no feature location "+
						feature.getName()+" found on reference sequence "+constrainingReference.getName());
			} else {
				throw new FeatureProviderException(Code.FEATURE_LOCATION_NOT_FOUND_ON_CONSTRAINING_REFERENCE, 
						feature.getName(), constrainingReference.getName());
			}
		}
		List<ReferenceSegment> fLocSegments = featureLocation.segmentsAsReferenceSegments();
		int referenceFLocNts = IReferenceSegment.totalReferenceLength(fLocSegments);
		if(referenceFLocNts == 0) {
			if(featureLocationMayBeAbsent) {
				GlueLogger.log(Level.FINEST, "No GenBank feature generated for sequence "+
						sequence.getSource().getName()+"/"+sequence.getSequenceID()+" for feature location "+
						featureLocation.pkMap()+"; it was empty on the reference sequence");
				return null;
			} else {
				throw new FeatureProviderException(Code.FEATURE_LOCATION_EMPTY_ON_CONSTRAINING_REFERENCE, 
						feature.getName(), constrainingReference.getName());
			}
		}
		
		List<QueryAlignedSegment> allMemberSegments = almtMember.segmentsAsQueryAlignedSegments();
		List<QueryAlignedSegment> memberFeatureSegments = ReferenceSegment.intersection(allMemberSegments, fLocSegments, ReferenceSegment.cloneLeftSegMerger());
		
		int memberFLocNts = IReferenceSegment.totalReferenceLength(memberFeatureSegments);
		
		double memberCoveragePct = 100.0 * ((double) memberFLocNts / (double) referenceFLocNts);
		
		Double minCoveragePct = getMinCoveragePct();
		if(memberCoveragePct < minCoveragePct) {
			GlueLogger.log(Level.FINEST, "No GenBank feature generated for sequence "+
					sequence.getSource().getName()+"/"+sequence.getSequenceID()+" based on feature location "+
					featureLocation.pkMap()+": member coverage percent "+
					memberCoveragePct+" was less than the minimum "+minCoveragePct);
			return null;
		}
		String featureKey = getFeatureKey();
		Map<String, String> qualifierKeyValues = generateQualifierKeyValuesFromFeatureLocation(cmdContext, featureLocation); 
		List<GbFeatureInterval> gbFeatureIntervals = new ArrayList<GbFeatureInterval>();
		
		
		for(int i = 0; i < fLocSegments.size(); i++) {
			ReferenceSegment fLocSegment = fLocSegments.get(i);
			List<QueryAlignedSegment> memberFLocSegments = 
					ReferenceSegment.intersection(allMemberSegments, Arrays.asList(fLocSegment), ReferenceSegment.cloneLeftSegMerger());
			if(getSpanInsertions()) {
				memberFLocSegments = Arrays.asList(new QueryAlignedSegment(
						ReferenceSegment.minRefStart(memberFLocSegments), 
						ReferenceSegment.maxRefEnd(memberFLocSegments), 
						QueryAlignedSegment.minQueryStart(memberFLocSegments), 
						QueryAlignedSegment.maxQueryEnd(memberFLocSegments)));
			}
			for(int j = 0 ; j < memberFLocSegments.size(); j++) {
				QueryAlignedSegment memberFLocSegment = memberFLocSegments.get(j);
				int startNt = memberFLocSegment.getQueryStart();
				boolean incompleteStart = false;
				int endNt = memberFLocSegment.getQueryEnd();
				boolean incompleteEnd = false; 
				int refStartNt = memberFLocSegment.getRefStart();
				
				if(i == 0 && j == 0 && 
						memberFLocSegment.getRefStart() > fLocSegment.getRefStart()) {
					incompleteStart = true;
				}
				if(i == fLocSegments.size() - 1 && j == memberFLocSegments.size() - 1 && 
						memberFLocSegment.getRefEnd() < fLocSegment.getRefEnd()) {
					incompleteEnd = true;
				}
				gbFeatureIntervals.add(new GbFeatureInterval(startNt, refStartNt, incompleteStart, endNt, incompleteEnd));
			}
			if(gbFeatureIntervals.size() == 0) {
				throw new FeatureProviderException(Code.NO_INTERVALS_GENERATED, sequence.getSource().getName()+"/"+sequence.getSequenceID(), featureKey);
			}
			// for incomplete starts of coding features, correct the reading frame as necessary.
			if(featureLocation.getFeature().codesAminoAcids() && featureKey.equals("CDS")) {
				GbFeatureInterval firstInterval = gbFeatureIntervals.get(0);
				
				if(firstInterval.isIncompleteStart()) {
					// location on reference where reading frame starts.
					Integer codon1Start = featureLocation.getCodon1Start(cmdContext);
					Integer refStartNt = firstInterval.getRefStartNt();
					if(!TranslationUtils.isAtStartOfCodon(codon1Start, refStartNt)) {
						if(TranslationUtils.isAtEndOfCodon(codon1Start, refStartNt)) {
							qualifierKeyValues.put("codon_start", "2");
						} else {
							qualifierKeyValues.put("codon_start", "3");
						}
					}
				}
			}

		}

		return new GbFeatureSpecification(gbFeatureIntervals, featureKey, qualifierKeyValues);
	}

	

	// If the sequence is a member of multiple alignment nodes in the given tree, a unique constrained alignment node 
	// which is a descendent of all the other nodes it is a member of is searched for. If no such node is found, an 
	// exception is thrown. 
	private AlignmentMember getUniqueAlignmentMembership(Sequence sequence, Alignment ancestorAlmt) {
		List<AlignmentMember> constrainedAlignmentMemberships = getConstrainedAlignmentMemberships(sequence, ancestorAlmt);
		if(constrainedAlignmentMemberships.size() == 0) {
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
