package uk.ac.gla.cvr.gluetools.core.reporting.custom;

import java.util.Optional;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@PluginClass(elemName="ravFinder")
public class RavFinder extends ModulePlugin<RavFinder> {

	public static final String GENOTYPE_MAX_PCT = "genotypeMaxPct";
	public static final String READS_MIN_PCT = "readsMinPct";
	
	private double genotypeMaxPct = 10.0;
	private double readsMinPct = 10.0;

	public RavFinder() {
		super();
		addModulePluginCmdClass(FindRavsCommand.class);
		addSimplePropertyName(GENOTYPE_MAX_PCT);
		addSimplePropertyName(READS_MIN_PCT);
	}

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		genotypeMaxPct = Optional.ofNullable(PluginUtils.configureDoubleProperty(configElem, GENOTYPE_MAX_PCT, false)).orElse(10.0);
		readsMinPct = Optional.ofNullable(PluginUtils.configureDoubleProperty(configElem, READS_MIN_PCT, false)).orElse(10.0);
	}

	public double getGenotypeMaxPct() {
		return genotypeMaxPct;
	}
	
	public double getReadsMinPct() {
		return readsMinPct;
	}

	
}
