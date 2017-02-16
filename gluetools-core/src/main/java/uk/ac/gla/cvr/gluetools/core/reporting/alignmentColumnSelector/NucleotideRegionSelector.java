package uk.ac.gla.cvr.gluetools.core.reporting.alignmentColumnSelector;

import java.util.Arrays;
import java.util.List;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;

@PluginClass(elemName="nucleotideRegionSelector")
public class NucleotideRegionSelector extends RegionSelector {

	private String featureName;
	private Integer startNt;
	private Integer endNt;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.featureName = PluginUtils.configureStringProperty(configElem, "featureName", true);
		this.startNt = PluginUtils.configureIntProperty(configElem, "startNt", false);
		this.endNt = PluginUtils.configureIntProperty(configElem, "endNt", false);
	}

	@Override
	protected List<ReferenceSegment> selectAlignmentColumnsInternal(CommandContext cmdContext, String relRefName) {
		return selectAlignmentColumns(cmdContext, relRefName, featureName, startNt, endNt);
	}

	public static List<ReferenceSegment> selectAlignmentColumns(CommandContext cmdContext, String relRefName, String featureName, Integer startNt, Integer endNt) {
		int startNtToUse;
		int endNtToUse;
		
		FeatureLocation featureLoc = GlueDataObject.lookup(cmdContext, FeatureLocation.class, FeatureLocation.pkMap(relRefName, featureName));

		
		List<ReferenceSegment> featureRefSegs = featureLoc.segmentsAsReferenceSegments();
		if(startNt != null) {
			startNtToUse = startNt;
		} else {
			startNtToUse = ReferenceSegment.minRefStart(featureRefSegs);
		}
		if(endNt != null) {
			endNtToUse = endNt;
		} else {
			endNtToUse = ReferenceSegment.maxRefEnd(featureRefSegs);
		}
		return ReferenceSegment
				.intersection(featureRefSegs, Arrays.asList(new ReferenceSegment(startNtToUse, endNtToUse)), ReferenceSegment.cloneLeftSegMerger());
	}


}
