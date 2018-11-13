package uk.ac.gla.cvr.gluetools.core.treeVisualiser;

import java.util.Optional;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@PluginClass(elemName = "treeVisualiser", 
description="Renders GLUE command document tree into a command document form with many visualisation calculations already performed")
public class TreeVisualiser extends ModulePlugin<TreeVisualiser> {

	private double rootPct;
	private double leafInfoPct;
	private double leftMarginPct;
	private double rightMarginPct;
	private double topMarginPct;
	private double bottomMarginPct;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.rootPct = Optional.ofNullable(PluginUtils.configureDoubleProperty(configElem, "rootPct", false)).orElse(5.0);
		this.leafInfoPct = Optional.ofNullable(PluginUtils.configureDoubleProperty(configElem, "leafInfoPct", false)).orElse(10.0);
		this.leftMarginPct = Optional.ofNullable(PluginUtils.configureDoubleProperty(configElem, "leftMarginPct", false)).orElse(2.0);
		this.rightMarginPct = Optional.ofNullable(PluginUtils.configureDoubleProperty(configElem, "rightMarginPct", false)).orElse(2.0);
		this.topMarginPct = Optional.ofNullable(PluginUtils.configureDoubleProperty(configElem, "topMarginPct", false)).orElse(2.0);
		this.bottomMarginPct = Optional.ofNullable(PluginUtils.configureDoubleProperty(configElem, "bottomMarginPct", false)).orElse(2.0);
	}

	public TreeVisualiser() {
		super();
		registerModulePluginCmdClass(VisualiseTreeDocumentCommand.class);
	}

	public double getRootPct() {
		return rootPct;
	}

	public double getLeafInfoPct() {
		return leafInfoPct;
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

}
