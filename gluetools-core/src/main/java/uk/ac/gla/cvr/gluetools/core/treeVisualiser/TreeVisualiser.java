package uk.ac.gla.cvr.gluetools.core.treeVisualiser;

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
	private double leafTextGapPct;
	private String leafTextFont;
	private double maxVerticalLeafUnitSpacePx;
	private int minCollapsedSubtreeLeafUnits;
	private int maxCollapsedSubtreeLeafUnits;
	private List<MemberAnnotationGenerator> memberAnnotationGenerators;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.rootPct = Optional.ofNullable(PluginUtils.configureDoubleProperty(configElem, "rootPct", false)).orElse(5.0);
		this.leftMarginPct = Optional.ofNullable(PluginUtils.configureDoubleProperty(configElem, "leftMarginPct", false)).orElse(2.0);
		this.rightMarginPct = Optional.ofNullable(PluginUtils.configureDoubleProperty(configElem, "rightMarginPct", false)).orElse(2.0);
		this.topMarginPct = Optional.ofNullable(PluginUtils.configureDoubleProperty(configElem, "topMarginPct", false)).orElse(2.0);
		this.bottomMarginPct = Optional.ofNullable(PluginUtils.configureDoubleProperty(configElem, "bottomMarginPct", false)).orElse(2.0);
		this.leafTextGapPct = Optional.ofNullable(PluginUtils.configureDoubleProperty(configElem, "leafTextGapPct", false)).orElse(0.5);
		this.leafTextHeightProportion = Optional.ofNullable(PluginUtils.configureDoubleProperty(configElem, "leafTextHeightProportion", false)).orElse(0.9);
		this.leafTextFont = Optional.ofNullable(PluginUtils.configureStringProperty(configElem, "leafTextFont", false)).orElse("Arial");
		this.maxVerticalLeafUnitSpacePx = Optional.ofNullable(PluginUtils.configureDoubleProperty(configElem, "maxVerticalLeafSpacePx", false)).orElse(20.0);
		this.minCollapsedSubtreeLeafUnits = Optional.ofNullable(PluginUtils.configureIntProperty(configElem, "minCollapsedSubtreeLeafUnits", false)).orElse(1);
		this.maxCollapsedSubtreeLeafUnits = Optional.ofNullable(PluginUtils.configureIntProperty(configElem, "maxCollapsedSubtreeLeafUnits", false)).orElse(6);
		
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

	public Double getMaxVerticalLeafUnitSpacePx() {
		return maxVerticalLeafUnitSpacePx;
	}

	public int getMinCollapsedSubtreeLeafUnits() {
		return minCollapsedSubtreeLeafUnits;
	}

	public int getMaxCollapsedSubtreeLeafUnits() {
		return maxCollapsedSubtreeLeafUnits;
	}

}
