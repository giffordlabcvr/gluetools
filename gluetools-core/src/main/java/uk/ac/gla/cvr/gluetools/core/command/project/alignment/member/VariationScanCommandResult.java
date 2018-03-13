package uk.ac.gla.cvr.gluetools.core.command.project.alignment.member;

import java.util.List;

import uk.ac.gla.cvr.gluetools.core.command.result.BaseTableResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.Variation;
import uk.ac.gla.cvr.gluetools.core.variationscanner.VariationScanResult;

public class VariationScanCommandResult extends BaseTableResult<VariationScanResult<?>> {

	public VariationScanCommandResult(List<VariationScanResult<?>> rowObjects) {
		super("variationScanCommandResult", rowObjects, 
				column("variationRefSeq", vsr -> vsr.getVariationPkMap().get(Variation.REF_SEQ_NAME_PATH)),
				column("variationFeature", vsr -> vsr.getVariationPkMap().get(Variation.FEATURE_NAME_PATH)),
				column("variationName", vsr -> vsr.getVariationPkMap().get(Variation.NAME_PROPERTY)),
				column("sufficientCoverage", vsr -> vsr.isSufficientCoverage()),
				column("present", vsr -> vsr.isPresent())
		);
	}


}
