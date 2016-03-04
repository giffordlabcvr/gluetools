package uk.ac.gla.cvr.gluetools.core.command.project.alignment.member;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.codonNumbering.LabeledAminoAcid;
import uk.ac.gla.cvr.gluetools.core.codonNumbering.LabeledCodon;
import uk.ac.gla.cvr.gluetools.core.codonNumbering.LabeledQueryAminoAcid;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
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
import uk.ac.gla.cvr.gluetools.core.transcription.CommandContextTranslator;
import uk.ac.gla.cvr.gluetools.core.transcription.TranslationUtils;
import uk.ac.gla.cvr.gluetools.core.transcription.Translator;

@CommandClass(
		commandWords={"amino-acid"}, 
		description = "Translate a member sequence to amino acids", 
		docoptUsages = { "-r <acRefName> -f <featureName>" },
		docoptOptions = { 
		"-r <acRefName>, --acRefName <acRefName>        Ancestor-constraining ref",
		"-f <featureName>, --featureName <featureName>  Feature to translate",
		},
		furtherHelp = 
		"The <acRefName> argument names a reference sequence constraining an ancestor alignment of this member's alignment. "+
		"The <featureName> argument names a feature location which is defined on this reference. "+
		"The result will be confined to this feature location",
		metaTags = {}	
)
public class MemberAminoAcidCommand extends MemberModeCommand<MemberAminoAcidResult> {

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

	@Override
	public MemberAminoAcidResult execute(CommandContext cmdContext) {
		AlignmentMember almtMember = lookupMember(cmdContext);
		Alignment alignment = almtMember.getAlignment();
		ReferenceSequence ancConstrainingRef = alignment.getAncConstrainingRef(cmdContext, referenceName);
		FeatureLocation scannedFeatureLoc = 
				GlueDataObject.lookup(cmdContext, FeatureLocation.class, FeatureLocation.pkMap(referenceName, featureName), false);
		Feature feature = scannedFeatureLoc.getFeature();
		feature.checkCodesAminoAcids();
		return memberAminoAcids(cmdContext, almtMember, ancConstrainingRef, scannedFeatureLoc);
	}

	public static MemberAminoAcidResult memberAminoAcids(CommandContext cmdContext,
			AlignmentMember almtMember, ReferenceSequence ancConstrainingRef, FeatureLocation featureLoc) {
		Alignment tipAlmt = almtMember.getAlignment();
		
		Alignment ancestorAlignment = tipAlmt.getAncestorWithReferenceName(ancConstrainingRef.getName());

		
		List<QueryAlignedSegment> memberToConstrainingRefSegs = almtMember.segmentsAsQueryAlignedSegments();
		List<QueryAlignedSegment> memberToAncConstrRefSegsFull = tipAlmt.translateToAncConstrainingRef(cmdContext, memberToConstrainingRefSegs, ancConstrainingRef);

		// trim down to the feature area.
		List<ReferenceSegment> featureLocRefSegs = featureLoc.segmentsAsReferenceSegments();
		
		List<QueryAlignedSegment> memberToFeatureLocRefSegs = ReferenceSegment.intersection(memberToAncConstrRefSegsFull, featureLocRefSegs,
				ReferenceSegment.cloneLeftSegMerger());
		
		Integer codon1Start = featureLoc.getCodon1Start(cmdContext);
		List<QueryAlignedSegment> memberToAncConstrRefSegsCodonAligned = TranslationUtils.truncateToCodonAligned(codon1Start, memberToFeatureLocRefSegs);

		final Translator translator = new CommandContextTranslator(cmdContext);

		Sequence memberSequence = almtMember.getSequence();
		AbstractSequenceObject memberSeqObj = memberSequence.getSequenceObject();

		
		// build a map from anc ref NT to labeled codon;
		int ntStart = Integer.MAX_VALUE;
		int ntEnd = Integer.MIN_VALUE;
		for(QueryAlignedSegment qaSeg: memberToAncConstrRefSegsCodonAligned) {
			ntStart = Math.min(ntStart, qaSeg.getRefStart());
			ntEnd = Math.max(ntEnd, qaSeg.getRefEnd());
		}
		List<LabeledCodon> labeledCodons = ancestorAlignment.labelCodons(cmdContext, featureLoc.getFeature().getName(), ntStart, ntEnd);
		TIntObjectMap<LabeledCodon> ancRefNtToLabeledCodon = new TIntObjectHashMap<LabeledCodon>();
		for(LabeledCodon labeledCodon: labeledCodons) {
			ancRefNtToLabeledCodon.put(labeledCodon.getNtStart(), labeledCodon);
		}
		
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

		return new MemberAminoAcidResult(labeledQueryAminoAcids);
	}

	@CompleterClass
	public static final class Completer extends FeatureOfAncConstrainingRefCompleter {}

	
}
