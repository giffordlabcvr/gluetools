package uk.ac.gla.cvr.gluetools.core.reporting.memberAnnotationGenerator;

import java.util.Arrays;
import java.util.List;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.alignment.member.MemberVariationScanCommand;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.Variation;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.variationscanner.VariationScanResult;

@PluginClass(elemName="variationPresenceAnnotationGenerator")
public class VariationPresenceAnnotationGenerator extends MemberAnnotationGenerator {

	public static final String REL_REF_NAME = "relRefName";
	public static final String FEATURE_NAME = "featureName";
	public static final String VARIATION_NAME = "variationName";
	
	private String relRefName;
	private String featureName;
	private String variationName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.relRefName = PluginUtils.configureStringProperty(configElem, REL_REF_NAME, true);
		this.featureName = PluginUtils.configureStringProperty(configElem, FEATURE_NAME, true);
		this.variationName = PluginUtils.configureStringProperty(configElem, VARIATION_NAME, true);
	}

	
	@Override
	public String renderAnnotation(CommandContext cmdContext, AlignmentMember almtMember) {
		Alignment alignment = almtMember.getAlignment();
		ReferenceSequence relatedRef = alignment.getRelatedRef(cmdContext, relRefName);
		
		FeatureLocation featureLoc = 
				GlueDataObject.lookup(cmdContext, FeatureLocation.class, FeatureLocation.pkMap(relRefName, featureName), false);

		Variation variation = 
				GlueDataObject.lookup(cmdContext, Variation.class, Variation.pkMap(relRefName, featureName, variationName), false);

		List<VariationScanResult<?>> scanResults = MemberVariationScanCommand.memberVariationScan(cmdContext, almtMember, relatedRef, featureLoc, Arrays.asList(variation), false, false);
		
		VariationScanResult<?> vsr = scanResults.get(0);

		if(!vsr.isSufficientCoverage()) {
			return "-";
		}
		if(vsr.isPresent()) {
			return "present";
		} else {
			return "absent";
		}
	}

}
