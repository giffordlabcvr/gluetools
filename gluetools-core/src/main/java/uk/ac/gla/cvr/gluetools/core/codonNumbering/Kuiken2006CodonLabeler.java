package uk.ac.gla.cvr.gluetools.core.codonNumbering;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.codonNumbering.Kuiken2006CodonLabelerException.Code;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;
import uk.ac.gla.cvr.gluetools.core.transcription.TranslationUtils;

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
	public List<LabeledCodon> numberCodons(CommandContext cmdContext, Alignment alignment, String featureName, int ntStart, int ntEnd) {
		ReferenceSequence constrainingRef = alignment.getConstrainingRef();
		ReferenceSequence rootReference = alignment.getAncConstrainingRef(cmdContext, rootReferenceName);
		FeatureLocation constrainingFeatureLoc = 
				GlueDataObject.lookup(cmdContext, FeatureLocation.class, FeatureLocation.pkMap(constrainingRef.getName(), featureName));
		FeatureLocation rootFeatureLoc = 
				GlueDataObject.lookup(cmdContext, FeatureLocation.class, FeatureLocation.pkMap(rootReference.getName(), featureName));
		
		List<QueryAlignedSegment> constrainingRefToSelfSegs = constrainingFeatureLoc.getSegments().stream()
				.map(seg -> new QueryAlignedSegment(seg.getRefStart(), seg.getRefEnd(), seg.getRefStart(), seg.getRefEnd()))
				.collect(Collectors.toList());
		
		List<QueryAlignedSegment> constrainingRefToRootFull = 
				alignment.translateToAncConstrainingRef(cmdContext, constrainingRefToSelfSegs, rootReference);
		
		List<QueryAlignedSegment> constrainingRefToRootInverted = constrainingRefToRootFull.stream()
				.map(s -> s.invert()).collect(Collectors.toList());
		
		ReferenceSegment areaOfInterest = new ReferenceSegment(ntStart, ntEnd);

		Integer constrainingCodon1Start = constrainingFeatureLoc.getCodon1Start(cmdContext);
		Integer rootCodon1Start = rootFeatureLoc.getCodon1Start(cmdContext);
		
		constrainingRefToRootInverted = 
				ReferenceSegment.intersection(constrainingRefToRootInverted, Arrays.asList(areaOfInterest), ReferenceSegment.cloneLeftSegMerger());

		List<QueryAlignedSegment> constrainingRefToRootSegs = constrainingRefToRootInverted.stream()
				.map(s -> s.invert()).collect(Collectors.toList());
		
		List<LabeledCodon> numberedCodons = new ArrayList<LabeledCodon>();
		
		Integer lastRootLocation = null;
		Integer lastConstrainingRefLocation = null;
		QueryAlignedSegment currentSegment = null;
		
		
		if(constrainingRefToRootSegs.isEmpty() || constrainingRefToRootSegs.get(0).getQueryStart() > ntStart) {
			throw new Kuiken2006CodonLabelerException(Code.GAP_AT_START, rootReferenceName, ntStart);
		}
		
		for(int i = ntStart; i <= ntEnd; i++) {
			if(currentSegment != null) {
				if(i > currentSegment.getQueryEnd()) {
					currentSegment = null;
				} 
			}
			if(currentSegment == null && !constrainingRefToRootSegs.isEmpty()) {
				if(i >= constrainingRefToRootSegs.get(0).getQueryStart() &&
						i <= constrainingRefToRootSegs.get(0).getQueryEnd()) {
					currentSegment = constrainingRefToRootSegs.remove(0);
				}
			}
			if(currentSegment != null) {
				lastConstrainingRefLocation = i;
				lastRootLocation = i+currentSegment.getQueryToReferenceOffset();
			}	
			LabeledCodon labeledCodon = null;
			if(currentSegment != null) {
				if(TranslationUtils.isAtStartOfCodon(constrainingCodon1Start, i)) {
					int rootCodon = TranslationUtils.getCodon(rootCodon1Start, 
							i+currentSegment.getQueryToReferenceOffset());
					labeledCodon = new LabeledCodon(Integer.toString(rootCodon), i);
				}
			} else {
				if(TranslationUtils.isAtStartOfCodon(constrainingCodon1Start, i)) {
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
}
