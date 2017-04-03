package uk.ac.gla.cvr.gluetools.core.codonNumbering;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.codonNumbering.Kuiken2006CodonLabelerException.Code;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.feature.Feature;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;
import uk.ac.gla.cvr.gluetools.core.translation.TranslationUtils;

@PluginClass(elemName="kuiken2006CodonLabeler")
public class Kuiken2006CodonLabeler extends ModulePlugin<Kuiken2006CodonLabeler> implements CodonLabeler {

	private static final String ROOT_REFERENCE_NAME = "rootReferenceName";
	private String rootReferenceName;
	
	public Kuiken2006CodonLabeler() {
		super();
		addSimplePropertyName(ROOT_REFERENCE_NAME);
	}

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		rootReferenceName = PluginUtils.configureStringProperty(configElem, ROOT_REFERENCE_NAME, true);
	}

	@Override
	public List<LabeledCodon> labelCodons(CommandContext cmdContext, FeatureLocation featureLoc) {
		Integer ntStart = ReferenceSegment.minRefStart(featureLoc.getSegments());
		Integer ntEnd = ReferenceSegment.maxRefEnd(featureLoc.getSegments());

		Feature feature = featureLoc.getFeature();
		ReferenceSequence tipReference = featureLoc.getReferenceSequence();

		Integer tipCodon1Start = featureLoc.getCodon1Start(cmdContext);
		Integer rootCodon1Start;

		List<QueryAlignedSegment> tipRefFeatureToRootRefSegs;
		if(tipReference.getName().equals(rootReferenceName)) {
			tipRefFeatureToRootRefSegs = new ArrayList<QueryAlignedSegment>(Arrays.asList(new QueryAlignedSegment(ntStart, ntEnd, ntStart, ntEnd)));
			rootCodon1Start = tipCodon1Start;
		} else {
			AlignmentMember tipAlmtMember = null;

			List<Alignment> almtsConstrainedByTipRef = tipReference.getAlignmentsWhereRefSequence();
			if(almtsConstrainedByTipRef.size() == 1) {
				// this reference constrains a single alignment.
				Alignment almtConstrainedByRef = almtsConstrainedByTipRef.get(0);
				Alignment parent = almtConstrainedByRef.getParent();
				if(parent != null) {
					tipAlmtMember = tipReference.getTipAlignmentMembership(parent.getName());
				}
			}
			if(tipAlmtMember == null) {
				tipAlmtMember = tipReference.getUniqueTipAlignmentMembership();
			}

			Alignment tipAlignment = tipAlmtMember.getAlignment();

			ReferenceSequence rootReference = tipAlignment.getAncConstrainingRef(cmdContext, rootReferenceName);
			FeatureLocation rootFeatureLoc = 
					GlueDataObject.lookup(cmdContext, FeatureLocation.class, FeatureLocation.pkMap(rootReference.getName(), feature.getName()));

			ReferenceSequence constrainingReference = tipAlignment.getConstrainingRef();
			FeatureLocation constrainingFeatureLoc = 
					GlueDataObject.lookup(cmdContext, FeatureLocation.class, FeatureLocation.pkMap(constrainingReference.getName(), feature.getName()));


			List<QueryAlignedSegment> tipRefToConstrainingRefSegs = tipAlmtMember.segmentsAsQueryAlignedSegments();

			List<QueryAlignedSegment> tipRefFeatureToConstrRefSegs = 
					ReferenceSegment.intersection(tipRefToConstrainingRefSegs, constrainingFeatureLoc.getSegments(), ReferenceSegment.cloneLeftSegMerger());

			tipRefFeatureToRootRefSegs = 
					tipAlignment.translateToAncConstrainingRef(cmdContext, tipRefFeatureToConstrRefSegs, rootReference);

			rootCodon1Start = rootFeatureLoc.getCodon1Start(cmdContext);
		}

		List<LabeledCodon> numberedCodons = new ArrayList<LabeledCodon>();

		Integer lastRootLocation = null;
		Integer lastConstrainingRefLocation = null;
		QueryAlignedSegment currentSegment = null;


		if(tipRefFeatureToRootRefSegs.isEmpty() || tipRefFeatureToRootRefSegs.get(0).getQueryStart() > ntStart) {
			throw new Kuiken2006CodonLabelerException(Code.GAP_AT_START, rootReferenceName, ntStart);
		}

		for(int i = ntStart; i <= ntEnd; i++) {
			if(currentSegment != null) {
				if(i > currentSegment.getQueryEnd()) {
					currentSegment = null;
				} 
			}
			if(currentSegment == null && !tipRefFeatureToRootRefSegs.isEmpty()) {
				if(i >= tipRefFeatureToRootRefSegs.get(0).getQueryStart() &&
						i <= tipRefFeatureToRootRefSegs.get(0).getQueryEnd()) {
					currentSegment = tipRefFeatureToRootRefSegs.remove(0);
				}
			}
			if(currentSegment != null) {
				lastConstrainingRefLocation = i;
				lastRootLocation = i+currentSegment.getQueryToReferenceOffset();
			}	
			LabeledCodon labeledCodon = null;
			if(currentSegment != null) {
				if(TranslationUtils.isAtStartOfCodon(tipCodon1Start, i)) {
					int rootCodon = TranslationUtils.getCodon(rootCodon1Start, 
							i+currentSegment.getQueryToReferenceOffset());
					labeledCodon = new LabeledCodon(Integer.toString(rootCodon), i);
				}
			} else {
				if(TranslationUtils.isAtStartOfCodon(tipCodon1Start, i)) {
					int diff = i-lastConstrainingRefLocation;
					int lastRootCodon = TranslationUtils.getCodon(rootCodon1Start, lastRootLocation);
					int constrainingCodon = TranslationUtils.getCodon(rootCodon1Start, lastRootLocation+diff);
					String label = Integer.toString(lastRootCodon) + getAlpha(constrainingCodon - lastRootCodon);
					labeledCodon = new LabeledCodon(label, i);
				}
			}
			if(labeledCodon != null) {
				numberedCodons.add(labeledCodon);
			}
		}
		return numberedCodons;
	}

	private String getAlpha(int num) {
		String result = "";
		while (num > 0) {
			num--; // 1 => a, not 0 => a
			int remainder = num % 26;
			char digit = (char) (remainder + 97);
			result = digit + result;
			num = (num - remainder) / 26;
		}
		return result;
	}

	@Override
	public int compareCodonLabels(String codonLabel1, String codonLabel2) {
		Integer number1 = null;
		String alpha1 = null;
		Integer splitPoint1 = null;
		for(int i = 0; i < codonLabel1.length(); i++) {
			if(Character.isAlphabetic(codonLabel1.charAt(i))) {
				splitPoint1 = i;
			}
		}
		if(splitPoint1 != null) {
			number1 = Integer.parseInt(codonLabel1.substring(0, splitPoint1));
			alpha1 = codonLabel1.substring(splitPoint1);
		} else {
			number1 = Integer.parseInt(codonLabel1);
		}
		Integer number2 = null;
		String alpha2 = null;
		Integer splitPoint2 = null;
		for(int i = 0; i < codonLabel2.length(); i++) {
			if(Character.isAlphabetic(codonLabel2.charAt(i))) {
				splitPoint2 = i;
			}
		}
		if(splitPoint2 != null) {
			number2 = Integer.parseInt(codonLabel2.substring(0, splitPoint2));
			alpha2 = codonLabel2.substring(splitPoint2);
		} else {
			number2 = Integer.parseInt(codonLabel2);
		}
		int comp = Integer.compare(number1, number2);
		if(comp != 0) { return comp; }
		if(alpha1 == null && alpha2 == null) {
			return 0;
		}
		if(alpha1 == null && alpha2 != null) {
			return -1;
		}
		if(alpha1 != null && alpha2 == null) {
			return 1;
		}
		return alpha1.compareTo(alpha2);
	}

	public String getRootReferenceName() {
		return rootReferenceName;
	}

	
	
}
