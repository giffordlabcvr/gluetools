package uk.ac.gla.cvr.gluetools.core.command.project.alignment.member;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.cayenne.exp.Expression;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.feature.Feature;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.Variation;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.VariationException;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.VariationException.Code;
import uk.ac.gla.cvr.gluetools.core.variationscanner.VariationScanRenderHints;
import uk.ac.gla.cvr.gluetools.core.variationscanner.VariationScannerMatchResult;

public class VariationScanUtils {

	public interface VariationConsumer {
		public void consumeVariations(ReferenceSequence refToScan, FeatureLocation featureLoc, List<Variation> variationsToScan);
	}

	public static void visitVariations(CommandContext cmdContext, List<ReferenceSequence> refsToScan, 
			List<Feature> featuresToScan, Expression whereClause, VariationScanUtils.VariationConsumer variationConsumer) {
	for(ReferenceSequence refToScan: refsToScan) {
			
			for(Feature featureToScan: featuresToScan) {
				FeatureLocation featureLoc = 
						GlueDataObject.lookup(cmdContext, FeatureLocation.class, 
								FeatureLocation.pkMap(refToScan.getName(), featureToScan.getName()), true);
				if(featureLoc == null) {
					continue;
				}
				List<Variation> variationsToScan = featureLoc.getVariationsQualified(cmdContext, whereClause);
				if(variationsToScan == null) {
					continue;
				}
				variationConsumer.consumeVariations(refToScan, featureLoc, variationsToScan);
			}
		}
	}

	public static Class<? extends VariationScannerMatchResult> getMatchResultClass(
			CommandContext cmdContext, List<ReferenceSequence> refsToScan,
			List<Feature> featuresToScan, Expression whereClause) {
		Class<? extends VariationScannerMatchResult> matchResultClass;
		Set<Class<? extends VariationScannerMatchResult>> matchResultClasses = 
				new LinkedHashSet<Class<? extends VariationScannerMatchResult>>();
		visitVariations(cmdContext, refsToScan, featuresToScan, whereClause, new VariationConsumer() {
			@Override
			public void consumeVariations(ReferenceSequence refToScan,
					FeatureLocation featureLoc, List<Variation> variationsToScan) {
				if(!variationsToScan.isEmpty()) {
					matchResultClasses.add(VariationScanRenderHints.getMatchResultClass(variationsToScan));
				}
			}
		});
		if(matchResultClasses.size() > 1) {
			throw new VariationException(Code.VARIATIONS_OF_DIFFERENT_TYPES);
		}
		if(matchResultClasses.isEmpty()) {
			return null;
		}
		matchResultClass = matchResultClasses.iterator().next();
		return matchResultClass;
	}

}
