package uk.ac.gla.cvr.gluetools.core.genotyping.maxlikelihood;

import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.project.module.ModulePluginCommand;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

public abstract class AbstractGenotypeCommand extends ModulePluginCommand<GenotypeCommandResult, MaxLikelihoodGenotyper> {

	public static final String DETAIL_LEVEL = "detailLevel";
	
	public enum DetailLevel {
		LOW,
		MEDIUM,
		HIGH
	}
	
	private DetailLevel detailLevel = DetailLevel.LOW;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.detailLevel = Optional.ofNullable(PluginUtils.configureEnumProperty(DetailLevel.class, configElem, DETAIL_LEVEL, false)).orElse(detailLevel);
	}

	protected GenotypeCommandResult formResult(MaxLikelihoodGenotyper maxLikelihoodGenotyper, Map<String, QueryGenotypingResult> genotypeResults) {
		return new GenotypeCommandResult(maxLikelihoodGenotyper.getCladeCategories(), detailLevel, new ArrayList<QueryGenotypingResult>(genotypeResults.values()));
	}


}
