package uk.ac.gla.cvr.gluetools.core.command.project.alignment.member;

import gnu.trove.map.TIntObjectMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.codonNumbering.LabeledAminoAcid;
import uk.ac.gla.cvr.gluetools.core.codonNumbering.LabeledCodon;
import uk.ac.gla.cvr.gluetools.core.codonNumbering.LabeledQueryAminoAcid;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.feature.Feature;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.AbstractSequenceObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;
import uk.ac.gla.cvr.gluetools.core.translation.CommandContextTranslator;
import uk.ac.gla.cvr.gluetools.core.translation.TranslationUtils;
import uk.ac.gla.cvr.gluetools.core.translation.Translator;

public abstract class MemberBaseAminoAcidCommand<R extends CommandResult> extends MemberModeCommand<R> {


	public static final String AC_REF_NAME = "acRefName";
	public static final String FEATURE_NAME = "featureName";

	private String referenceName;
	private String featureName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.referenceName = PluginUtils.configureStringProperty(configElem, AC_REF_NAME, true);
		this.featureName = PluginUtils.configureStringProperty(configElem, FEATURE_NAME, true);
	}

	public static List<LabeledQueryAminoAcid> memberAminoAcids(CommandContext cmdContext,
			AlignmentMember almtMember, ReferenceSequence ancConstrainingRef, FeatureLocation featureLoc) {
		Alignment tipAlmt = almtMember.getAlignment();
		
		List<QueryAlignedSegment> memberToConstrainingRefSegs = almtMember.segmentsAsQueryAlignedSegments();
		List<QueryAlignedSegment> memberToAncConstrRefSegsFull = tipAlmt.translateToAncConstrainingRef(cmdContext, memberToConstrainingRefSegs, ancConstrainingRef);

		// trim down to the feature area.
		List<ReferenceSegment> featureLocRefSegs = featureLoc.segmentsAsReferenceSegments();
		
		List<QueryAlignedSegment> memberToFeatureLocRefSegs = ReferenceSegment.intersection(memberToAncConstrRefSegsFull, featureLocRefSegs,
				ReferenceSegment.cloneLeftSegMerger());
		
		// important to merge abutting here otherwise you may get gaps if the boundary is within a codon.
		List<QueryAlignedSegment> memberToFeatureLocRefSegsMerged = QueryAlignedSegment.mergeAbutting(memberToFeatureLocRefSegs, 
				QueryAlignedSegment.mergeAbuttingFunctionQueryAlignedSegment(), 
				QueryAlignedSegment.abutsPredicateQueryAlignedSegment());

		
		Integer codon1Start = featureLoc.getCodon1Start(cmdContext);
		List<QueryAlignedSegment> memberToAncConstrRefSegsCodonAligned = TranslationUtils.truncateToCodonAligned(codon1Start, memberToFeatureLocRefSegsMerged);

		final Translator translator = new CommandContextTranslator(cmdContext);

		Sequence memberSequence = almtMember.getSequence();
		AbstractSequenceObject memberSeqObj = memberSequence.getSequenceObject();

		if(memberToAncConstrRefSegsCodonAligned.isEmpty()) {
			return Collections.emptyList();
		}
		
		TIntObjectMap<LabeledCodon> ancRefNtToLabeledCodon = featureLoc.getRefNtToLabeledCodon(cmdContext);

		List<LabeledQueryAminoAcid> labeledQueryAminoAcids = new ArrayList<LabeledQueryAminoAcid>();

		
		for(QueryAlignedSegment memberToAncConstrRefSeg: memberToAncConstrRefSegsCodonAligned) {
			CharSequence nts = memberSeqObj.subSequence(cmdContext, 
					memberToAncConstrRefSeg.getQueryStart(), memberToAncConstrRefSeg.getQueryEnd());
			String segAAs = translator.translate(nts);
			int refNt = memberToAncConstrRefSeg.getRefStart();
			int memberNt = memberToAncConstrRefSeg.getQueryStart();
			for(int i = 0; i < segAAs.length(); i++) {
				String segAA = segAAs.substring(i, i+1);
				labeledQueryAminoAcids.add(new LabeledQueryAminoAcid(
						new LabeledAminoAcid(ancRefNtToLabeledCodon.get(refNt), segAA), memberNt));
				refNt = refNt+3;
				memberNt = memberNt+3;
			}
		}

		return labeledQueryAminoAcids;
	}
	
	protected List<LabeledQueryAminoAcid> getMemberAminoAcids(
			CommandContext cmdContext) {
		AlignmentMember almtMember = lookupMember(cmdContext);
		Alignment alignment = almtMember.getAlignment();
		ReferenceSequence ancConstrainingRef = alignment.getAncConstrainingRef(cmdContext, referenceName);
		FeatureLocation featureLoc = 
				GlueDataObject.lookup(cmdContext, FeatureLocation.class, FeatureLocation.pkMap(referenceName, featureName), false);
		Feature feature = featureLoc.getFeature();
		feature.checkCodesAminoAcids();
		List<LabeledQueryAminoAcid> memberAminoAcids = memberAminoAcids(cmdContext, almtMember, ancConstrainingRef, featureLoc);
		return memberAminoAcids;
	}




	
}
