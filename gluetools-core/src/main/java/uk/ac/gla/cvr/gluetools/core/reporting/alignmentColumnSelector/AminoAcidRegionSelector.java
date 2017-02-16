package uk.ac.gla.cvr.gluetools.core.reporting.alignmentColumnSelector;

import java.util.Arrays;
import java.util.List;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.codonNumbering.LabeledCodon;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CommandException.Code;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;

@PluginClass(elemName="aminoAcidRegionSelector")
public class AminoAcidRegionSelector extends RegionSelector {

	private String featureName;
	private String startCodon;
	private String endCodon;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.featureName = PluginUtils.configureStringProperty(configElem, "featureName", true);
		this.startCodon = PluginUtils.configureStringProperty(configElem, "startCodon", false);
		this.endCodon = PluginUtils.configureStringProperty(configElem, "endCodon", false);
	}

	@Override
	protected List<ReferenceSegment> selectAlignmentColumnsInternal(CommandContext cmdContext, String relRefName) {
		return selectAlignmentColumns(cmdContext, relRefName, featureName, startCodon, endCodon);
	}

	public static List<ReferenceSegment> selectAlignmentColumns(CommandContext cmdContext, String relRefName, String featureName, String startCodon, String endCodon) {
		FeatureLocation featureLoc = GlueDataObject.lookup(cmdContext, FeatureLocation.class, FeatureLocation.pkMap(relRefName, featureName));
		List<ReferenceSegment> featureRefSegs = featureLoc.segmentsAsReferenceSegments();
		featureLoc.getFeature().checkCodesAminoAcids();
		LabeledCodon startLabeledCodon;
		if(startCodon != null) {
			startLabeledCodon = featureLoc.getLabeledCodon(cmdContext, startCodon);
		} else {
			startLabeledCodon = featureLoc.getLabeledCodons(cmdContext).get(0); 
		}
		LabeledCodon endLabeledCodon;
		if(endCodon != null) {
			endLabeledCodon = featureLoc.getLabeledCodon(cmdContext, endCodon);
		} else {
			List<LabeledCodon> labeledCodons = featureLoc.getLabeledCodons(cmdContext);
			endLabeledCodon = labeledCodons.get(labeledCodons.size()-1); 
		}
		if(endLabeledCodon.getNtStart() < startLabeledCodon.getNtStart()) {
			throw new CommandException(Code.COMMAND_FAILED_ERROR, "Codon with label \""+endCodon+"\" occurs before codon with label \""+startCodon+"\"");
		}
		int lcRegionNtStart = startLabeledCodon.getNtStart();
		int lcRegionNtEnd = endLabeledCodon.getNtStart()+2;
		return ReferenceSegment
				.intersection(featureRefSegs, Arrays.asList(new ReferenceSegment(lcRegionNtStart, lcRegionNtEnd)), ReferenceSegment.cloneLeftSegMerger());

	}
	
	
}
