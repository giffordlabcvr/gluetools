package uk.ac.gla.cvr.gluetools.core.genotyping.maxlikelihood;

import java.io.File;
import java.util.Map;

import org.biojava.nbio.core.sequence.DNASequence;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.module.Module;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.core.placement.maxlikelihood.MaxLikelihoodPlacer;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@PluginClass(elemName="maxLikelihoodGenotyper")
public class MaxLikelihoodGenotyper extends ModulePlugin<MaxLikelihoodGenotyper> {

	public static final String MAX_LIKELIHOOD_PLACER_MODULE_NAME = "maxLikelihoodPlacerModuleName";
	
	private String maxLikelihoodPlacerModuleName;
	
	public MaxLikelihoodGenotyper() {
		super();
		addModulePluginCmdClass(GenotypeFileCommand.class);
		addModulePluginCmdClass(GenotypeSequenceCommand.class);
		addSimplePropertyName(MAX_LIKELIHOOD_PLACER_MODULE_NAME);
	}

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.maxLikelihoodPlacerModuleName = PluginUtils.configureStringProperty(configElem, MAX_LIKELIHOOD_PLACER_MODULE_NAME, true);
	}

	
	@Override
	public void validate(CommandContext cmdContext) {
		super.validate(cmdContext);
		MaxLikelihoodPlacer maxLikelihoodPlacer = resolvePlacer(cmdContext);
		maxLikelihoodPlacer.validate(cmdContext);
	}

	public Map<String, GenotypeResult> genotype(CommandContext cmdContext, Map<String, DNASequence> idToSequence, File dataDir) {
		MaxLikelihoodPlacer maxLikelihoodPlacer = resolvePlacer(cmdContext);
		return null;
	}
	
	private MaxLikelihoodPlacer resolvePlacer(CommandContext cmdContext) {
		MaxLikelihoodPlacer maxLikelihoodPlacer = 
				Module.resolveModulePlugin(cmdContext, MaxLikelihoodPlacer.class, maxLikelihoodPlacerModuleName);
		return maxLikelihoodPlacer;
	}
	
	
}
