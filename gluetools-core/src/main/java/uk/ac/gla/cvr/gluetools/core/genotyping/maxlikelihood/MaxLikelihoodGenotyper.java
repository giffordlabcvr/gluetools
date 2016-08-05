package uk.ac.gla.cvr.gluetools.core.genotyping.maxlikelihood;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.biojava.nbio.core.sequence.DNASequence;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.module.Module;
import uk.ac.gla.cvr.gluetools.core.genotyping.maxlikelihood.GenotypeResult.SummaryCode;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.core.placement.maxlikelihood.MaxLikelihoodPlacer;
import uk.ac.gla.cvr.gluetools.core.placement.maxlikelihood.PlacementResult;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@PluginClass(elemName="maxLikelihoodGenotyper")
public class MaxLikelihoodGenotyper extends ModulePlugin<MaxLikelihoodGenotyper> {

	public static final String MAX_LIKELIHOOD_PLACER_MODULE_NAME = "maxLikelihoodPlacerModuleName";
	public static final String MAX_DISTANCE = "maxDistance";
	
	private String maxLikelihoodPlacerModuleName;
	private Double maxDistance;
	
	public MaxLikelihoodGenotyper() {
		super();
		addModulePluginCmdClass(GenotypeFileCommand.class);
		addModulePluginCmdClass(GenotypeSequenceCommand.class);
		addSimplePropertyName(MAX_LIKELIHOOD_PLACER_MODULE_NAME);
		addSimplePropertyName(MAX_DISTANCE);
	}

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.maxLikelihoodPlacerModuleName = PluginUtils.configureStringProperty(configElem, MAX_LIKELIHOOD_PLACER_MODULE_NAME, true);
		this.maxDistance = Optional.ofNullable(PluginUtils.configureDoubleProperty(configElem, MAX_DISTANCE, false)).orElse(0.1);
	}

	
	@Override
	public void validate(CommandContext cmdContext) {
		super.validate(cmdContext);
		MaxLikelihoodPlacer maxLikelihoodPlacer = resolvePlacer(cmdContext);
		maxLikelihoodPlacer.validate(cmdContext);
	}

	public Map<String, GenotypeResult> genotype(CommandContext cmdContext, Map<String, DNASequence> idToSequence, File dataDir) {
		MaxLikelihoodPlacer maxLikelihoodPlacer = resolvePlacer(cmdContext);

		Map<String, List<PlacementResult>> idToPlacements = maxLikelihoodPlacer.place(cmdContext, idToSequence, dataDir);
		Map<String, GenotypeResult> idToResult = new LinkedHashMap<String, GenotypeResult>();

		idToPlacements.forEach((id, placements) -> {
			GenotypeResult genotypeResult = genotypeResultFromPlacements(cmdContext, id, placements);
			idToResult.put(id, genotypeResult);
		});
		
		return idToResult;
	}
	
	private GenotypeResult genotypeResultFromPlacements(CommandContext cmdContext, String id, List<PlacementResult> placementResults) {
		GenotypeResult bestGenotypeResult = new GenotypeResult(); // null result, in case placementResults is empty.
		bestGenotypeResult.setSequenceName(id);
		bestGenotypeResult.setSummaryCode(SummaryCode.NEGATIVE);
		for(PlacementResult placementResult : placementResults) {
			GenotypeResult genotypeResult = new GenotypeResult();
			genotypeResult.setSequenceName(id);
			Map<String, String> pkMap = placementResult.getClosestMemberPkMap();
			genotypeResult.setPlacementResult(placementResult);
			if(placementResult.getDistanceToClosestMember().doubleValue() <= maxDistance) {
				String closestMemberAlignment = pkMap.get(AlignmentMember.ALIGNMENT_NAME_PATH);
				genotypeResult.setTypeAlignmentName(closestMemberAlignment);
				if(placementResult.getGroupingAlignmentName().equals(closestMemberAlignment)) {
					genotypeResult.setSummaryCode(SummaryCode.POSITIVE_GROUPED);
				} else {
					genotypeResult.setSummaryCode(SummaryCode.POSITIVE_UNGROUPED);
				}
			} else {
				genotypeResult.setTypeAlignmentName(placementResult.getGroupingAlignmentName());
				genotypeResult.setSummaryCode(SummaryCode.OUTGROUP);
			}
			if(bestGenotypeResult.getSummaryCode() == SummaryCode.NEGATIVE) { // anything beats the null result.
				bestGenotypeResult = genotypeResult;
			} else {
				if(placementResult.getLikeWeightRatio() > bestGenotypeResult.getPlacementResult().getLikeWeightRatio()) {
					bestGenotypeResult = genotypeResult;
				}
			}
		}
		return bestGenotypeResult;
	}

	private MaxLikelihoodPlacer resolvePlacer(CommandContext cmdContext) {
		MaxLikelihoodPlacer maxLikelihoodPlacer = 
				Module.resolveModulePlugin(cmdContext, MaxLikelihoodPlacer.class, maxLikelihoodPlacerModuleName);
		return maxLikelihoodPlacer;
	}

}
