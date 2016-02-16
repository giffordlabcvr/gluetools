package uk.ac.gla.cvr.gluetools.core.command.project.alignment.member;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.CompletionSuggestion;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
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
		commandWords={"amino-acids"}, 
		description = "Translate the amino acids for a given feature location", 
		docoptUsages = { "<refName> <featureName>" },
		furtherHelp = 
		"The <refName> argument names a reference sequence constraining an ancestor alignment of this member's alignment. "+
		"The <featureName> arguments names a feature which is defined on this reference.",
		metaTags = {}	
)
public class MemberAminoAcidsCommand extends MemberModeCommand<MemberAminoAcidsResult> {

	public static final String REFERENCE_NAME = "refName";
	public static final String FEATURE_NAME = "featureName";

	private String referenceName;
	private String featureName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.referenceName = PluginUtils.configureStringProperty(configElem, REFERENCE_NAME, true);
		this.featureName = PluginUtils.configureStringProperty(configElem, FEATURE_NAME, true);
	}

	@Override
	public MemberAminoAcidsResult execute(CommandContext cmdContext) {
		AlignmentMember almtMember = lookupMember(cmdContext);
		Alignment alignment = almtMember.getAlignment();
		ReferenceSequence ancConstrainingRef = alignment.getAncConstrainingRef(cmdContext, referenceName);
		FeatureLocation scannedFeatureLoc = 
				GlueDataObject.lookup(cmdContext, FeatureLocation.class, FeatureLocation.pkMap(referenceName, featureName), false);
		Feature feature = scannedFeatureLoc.getFeature();
		feature.checkCodesAminoAcids();
		return memberAminoAcids(cmdContext, almtMember, ancConstrainingRef, scannedFeatureLoc);
	}

	public static MemberAminoAcidsResult memberAminoAcids(CommandContext cmdContext,
			AlignmentMember almtMember, ReferenceSequence ancConstrainingRef, FeatureLocation scannedFeatureLoc) {
		Alignment alignment = almtMember.getAlignment();
		List<QueryAlignedSegment> memberToConstrainingRefSegs = almtMember.segmentsAsQueryAlignedSegments();
		List<QueryAlignedSegment> memberToAncConstrainingRefSegs = alignment.translateToAncConstrainingRef(cmdContext, memberToConstrainingRefSegs, ancConstrainingRef);

		List<ReferenceSegment> featureLocRefSegs = scannedFeatureLoc.segmentsAsReferenceSegments();
		
		List<QueryAlignedSegment> memberToFeatureLocRefSegs = ReferenceSegment.intersection(memberToAncConstrainingRefSegs, featureLocRefSegs,
				ReferenceSegment.cloneLeftSegMerger());
		
		int codon1Start = scannedFeatureLoc.getCodon1Start(cmdContext);
		List<QueryAlignedSegment> memberToFeatureLocRefSegsCodonAligned = TranslationUtils.truncateToCodonAligned(codon1Start, memberToFeatureLocRefSegs);

		final Translator translator = new CommandContextTranslator(cmdContext);

		Sequence memberSequence = almtMember.getSequence();
		AbstractSequenceObject memberSeqObj = memberSequence.getSequenceObject();
		
		List<Map<String, Object>> rowData = new ArrayList<Map<String, Object>>();

		for(QueryAlignedSegment memberToRefSeg: memberToFeatureLocRefSegsCodonAligned) {
			CharSequence nts = memberSeqObj.subSequence(cmdContext, 
					memberToRefSeg.getQueryStart(), memberToRefSeg.getQueryEnd());
			String segAAs = translator.translate(nts);
			int codon = TranslationUtils.getCodon(codon1Start, memberToRefSeg.getRefStart());
			for(int i = 0; i < segAAs.length(); i++) {
				char segAA = segAAs.charAt(i);
				Map<String, Object> row = new LinkedHashMap<String, Object>();
				row.put(MemberAminoAcidsResult.CODON, codon);
				row.put(MemberAminoAcidsResult.AMINO_ACID, new String(new char[]{segAA}));
				rowData.add(row);
				codon++;
			}
		}

		return new MemberAminoAcidsResult(rowData);
	}

	@CompleterClass
	public static final class Completer extends FeatureOfAncConstrainingRefCompleter {}

	
}
