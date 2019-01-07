package uk.ac.gla.cvr.gluetools.core.treeVisualiser;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.reporting.memberAnnotationGenerator.MemberAnnotationGenerator;
import uk.ac.gla.cvr.gluetools.core.reporting.memberAnnotationGenerator.MemberAnnotationGeneratorFactory;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;

@PluginClass(elemName = "treeVisualiser", 
description="Renders GLUE command document tree into a command document form with many visualisation calculations already performed")
public class TreeVisualiser extends ModulePlugin<TreeVisualiser> {

	private double rootPct;
	private double leafTextHeightProportion;
	private double leftMarginPct;
	private double rightMarginPct;
	private double topMarginPct;
	private double bottomMarginPct;
	private double legendTopMarginPct;
	private double legendBottomMarginPct;
	private double leafTextGapPct;
	private String leafTextFont;
	private double maxVerticalLeafUnitSpacePx;
	private int minCollapsedSubtreeLeafUnits;
	private int maxCollapsedSubtreeLeafUnits;
	private List<MemberAnnotationGenerator> memberAnnotationGenerators;
	private List<Double> validBranchLengthLegendValues;
	private double branchLengthLegendMinPct;
	private double branchLengthLegendMaxPct;
	private int branchLengthLegendDivisions;
	private String legendTextFont;
	private String branchLengthLegendUnitsText;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.rootPct = Optional.ofNullable(PluginUtils.configureDoubleProperty(configElem, "rootPct", false)).orElse(5.0);
		this.leftMarginPct = Optional.ofNullable(PluginUtils.configureDoubleProperty(configElem, "leftMarginPct", false)).orElse(2.0);
		this.rightMarginPct = Optional.ofNullable(PluginUtils.configureDoubleProperty(configElem, "rightMarginPct", false)).orElse(2.0);
		this.topMarginPct = Optional.ofNullable(PluginUtils.configureDoubleProperty(configElem, "topMarginPct", false)).orElse(2.0);
		this.bottomMarginPct = Optional.ofNullable(PluginUtils.configureDoubleProperty(configElem, "bottomMarginPct", false)).orElse(2.0);
		this.legendTopMarginPct = Optional.ofNullable(PluginUtils.configureDoubleProperty(configElem, "legendTopMarginPct", false)).orElse(2.0);
		this.legendBottomMarginPct = Optional.ofNullable(PluginUtils.configureDoubleProperty(configElem, "legendBottomMarginPct", false)).orElse(2.0);
		this.leafTextGapPct = Optional.ofNullable(PluginUtils.configureDoubleProperty(configElem, "leafTextGapPct", false)).orElse(0.5);
		this.leafTextHeightProportion = Optional.ofNullable(PluginUtils.configureDoubleProperty(configElem, "leafTextHeightProportion", false)).orElse(0.9);
		this.leafTextFont = Optional.ofNullable(PluginUtils.configureStringProperty(configElem, "leafTextFont", false)).orElse("Arial");
		this.legendTextFont = Optional.ofNullable(PluginUtils.configureStringProperty(configElem, "legendTextFont", false)).orElse("Arial");
		this.maxVerticalLeafUnitSpacePx = Optional.ofNullable(PluginUtils.configureDoubleProperty(configElem, "maxVerticalLeafSpacePx", false)).orElse(22.0);
		this.minCollapsedSubtreeLeafUnits = Optional.ofNullable(PluginUtils.configureIntProperty(configElem, "minCollapsedSubtreeLeafUnits", false)).orElse(1);
		this.maxCollapsedSubtreeLeafUnits = Optional.ofNullable(PluginUtils.configureIntProperty(configElem, "maxCollapsedSubtreeLeafUnits", false)).orElse(6);
		this.validBranchLengthLegendValues = PluginUtils.configureDoublesProperty(configElem, "validBranchLengthLegendValues", 0, null, 0.0, false, null, false);
		if(this.validBranchLengthLegendValues.isEmpty()) {
			this.validBranchLengthLegendValues = Arrays.asList(
					0.000001,	0.0000025,	0.000005,
					0.00001,	0.000025,	0.00005,
					0.0001,		0.00025,	0.0005,
					0.001,		0.0025,		0.005,
					0.01,		0.025,		0.05,
					0.1,		0.25,		0.5,
					1.0,		2.5,		5.0,
					10.0,		25.0,		50.0,
					100.0,		250.0,		500.0,
					1000.0,		2500.0,		5000.0,
					10000.0,	25000.0,	50000.0
			);
		}
		this.branchLengthLegendMinPct = Optional.ofNullable(PluginUtils.configureDoubleProperty(configElem, "branchLengthLegendMinPct", false)).orElse(30.0);
		this.branchLengthLegendMaxPct = Optional.ofNullable(PluginUtils.configureDoubleProperty(configElem, "branchLengthLegendMaxPct", false)).orElse(90.0);
		this.branchLengthLegendDivisions = Optional.ofNullable(PluginUtils.configureIntProperty(configElem, "branchLengthLegendDivisions", false)).orElse(5);
		this.branchLengthLegendUnitsText = Optional.ofNullable(PluginUtils.configureStringProperty(configElem, "branchLengthLegendUnitsText", false))
				.orElse("Mean substitutions per site");
		
		MemberAnnotationGeneratorFactory annotationGeneratorFactory = PluginFactory.get(MemberAnnotationGeneratorFactory.creator);
		String alternateElemsXPath = GlueXmlUtils.alternateElemsXPath(annotationGeneratorFactory.getElementNames());
		List<Element> annotationGeneratorElems = PluginUtils.findConfigElements(configElem, alternateElemsXPath);
		this.memberAnnotationGenerators = annotationGeneratorFactory.createFromElements(pluginConfigContext, annotationGeneratorElems);

	}

	public TreeVisualiser() {
		super();
		registerModulePluginCmdClass(VisualiseTreeDocumentCommand.class);
	}

	public double getRootPct() {
		return rootPct;
	}


	public double getLeftMarginPct() {
		return leftMarginPct;
	}

	public double getRightMarginPct() {
		return rightMarginPct;
	}

	public double getTopMarginPct() {
		return topMarginPct;
	}

	public double getBottomMarginPct() {
		return bottomMarginPct;
	}
	
	public double getLegendTopMarginPct() {
		return legendTopMarginPct;
	}

	public double getLegendBottomMarginPct() {
		return legendBottomMarginPct;
	}
	
	public double getLeafTextHeightProportion() {
		return leafTextHeightProportion;
	}

	public double getLeafTextGapPct() {
		return leafTextGapPct;
	}

	public List<MemberAnnotationGenerator> getMemberAnnotationGenerators() {
		return memberAnnotationGenerators;
	}

	public String getLeafTextFont() {
		return leafTextFont;
	}
	
	public String getLegendTextFont() {
		return legendTextFont;
	}

	public Double getMaxVerticalLeafUnitSpacePx() {
		return maxVerticalLeafUnitSpacePx;
	}

	public int getMinCollapsedSubtreeLeafUnits() {
		return minCollapsedSubtreeLeafUnits;
	}

	public int getMaxCollapsedSubtreeLeafUnits() {
		return maxCollapsedSubtreeLeafUnits;
	}

	public List<Double> getValidBranchLengthLegendValues() {
		return validBranchLengthLegendValues;
	}

	public double getBranchLengthLegendMinPct() {
		return branchLengthLegendMinPct;
	}

	public double getBranchLengthLegendMaxPct() {
		return branchLengthLegendMaxPct;
	}

	public int getBranchLengthLegendDivisions() {
		return branchLengthLegendDivisions;
	}

	public String getBranchLengthLegendUnitsText() {
		return branchLengthLegendUnitsText;
	}

	
	
	
}
