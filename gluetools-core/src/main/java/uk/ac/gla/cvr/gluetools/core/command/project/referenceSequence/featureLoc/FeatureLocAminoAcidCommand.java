package uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.featureLoc;

import gnu.trove.map.TIntObjectMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.codonNumbering.LabeledAminoAcid;
import uk.ac.gla.cvr.gluetools.core.codonNumbering.LabeledCodon;
import uk.ac.gla.cvr.gluetools.core.codonNumbering.LabeledQueryAminoAcid;
import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
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
		description = "Translate the feature location to amino acids", 
		docoptUsages = { "" },
		docoptOptions = { },
		metaTags = {}	
)
public class FeatureLocAminoAcidCommand extends FeatureLocModeCommand<FeatureLocAminoAcidResult> {

	@Override
	public FeatureLocAminoAcidResult execute(CommandContext cmdContext) {
		FeatureLocation featureLoc = lookupFeatureLoc(cmdContext);
		List<LabeledAminoAcid> labeledAminoAcids = featureLocAminoAcids(cmdContext, featureLoc);
		return new FeatureLocAminoAcidResult(labeledAminoAcids);
	}

	public static List<LabeledAminoAcid> featureLocAminoAcids(CommandContext cmdContext, FeatureLocation featureLoc) {
		Feature feature = featureLoc.getFeature();
		feature.checkCodesAminoAcids();

		// feature area coordinates.
		List<ReferenceSegment> featureLocRefSegs = featureLoc.segmentsAsReferenceSegments();
		
		final Translator translator = new CommandContextTranslator(cmdContext);

		AbstractSequenceObject refSeqObj = featureLoc.getReferenceSequence().getSequence().getSequenceObject();

		if(featureLocRefSegs.isEmpty()) {
			return Collections.emptyList();
		}
		
		TIntObjectMap<LabeledCodon> refNtToLabeledCodon = featureLoc.getRefNtToLabeledCodon(cmdContext);

		List<LabeledAminoAcid> labeledAminoAcids = new ArrayList<LabeledAminoAcid>();

		for(ReferenceSegment featureLocRefSeg: featureLocRefSegs) {
			CharSequence nts = refSeqObj.subSequence(cmdContext, 
					featureLocRefSeg.getRefStart(), featureLocRefSeg.getRefEnd());
			String segAAs = translator.translate(nts);
			int refNt = featureLocRefSeg.getRefStart();
			for(int i = 0; i < segAAs.length(); i++) {
				String segAA = segAAs.substring(i, i+1);
				labeledAminoAcids.add(new LabeledAminoAcid(refNtToLabeledCodon.get(refNt), segAA));
				refNt = refNt+3;
			}
		}
		return labeledAminoAcids;
	}

	@CompleterClass
	public static final class Completer extends AdvancedCmdCompleter {}

	
}
