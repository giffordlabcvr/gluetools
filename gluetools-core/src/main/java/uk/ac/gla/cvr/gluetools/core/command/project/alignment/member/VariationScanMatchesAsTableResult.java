package uk.ac.gla.cvr.gluetools.core.command.project.alignment.member;

import java.util.ArrayList;
import java.util.List;

import uk.ac.gla.cvr.gluetools.core.command.result.BaseTableResult;
import uk.ac.gla.cvr.gluetools.core.command.result.TableColumn;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.Variation;
import uk.ac.gla.cvr.gluetools.core.variationscanner.VariationScanMatchResultRow;
import uk.ac.gla.cvr.gluetools.core.variationscanner.VariationScanResult;
import uk.ac.gla.cvr.gluetools.core.variationscanner.VariationScannerMatchResult;

public class VariationScanMatchesAsTableResult<M extends VariationScannerMatchResult> extends BaseTableResult<VariationScanMatchResultRow<M>> {

	public VariationScanMatchesAsTableResult(Class<M> matchResultClass, List<VariationScanResult<M>> vsrs) {
		super("variationScanMatchCommandResult", 
				resultRowsFromVariationScanResults(vsrs), 
				generateResultColumns(matchResultClass, 
						column("variationRefSeq", vsr -> vsr.getVariationPkMap().get(Variation.REF_SEQ_NAME_PATH)),
						column("variationFeature", vsr -> vsr.getVariationPkMap().get(Variation.FEATURE_NAME_PATH)),
						column("variationName", vsr -> vsr.getVariationPkMap().get(Variation.NAME_PROPERTY)),
						column("sufficientCoverage", vsr -> vsr.isSufficientCoverage()),
						column("present", vsr -> vsr.isPresent())));
	}

	@SuppressWarnings("unchecked")
	public static <M extends VariationScannerMatchResult> 
		List<VariationScanMatchResultRow<M>> resultRowsFromVariationScanResults(List<VariationScanResult<M>> variationScanResults) {
		List<VariationScanMatchResultRow<M>> resultRows = new ArrayList<VariationScanMatchResultRow<M>>();
		variationScanResults.forEach(vsr -> {
			List<M> variationScannerMatchResults = vsr.getVariationScannerMatchResults();
			variationScannerMatchResults.forEach(vsmr -> {
				resultRows.add(new VariationScanMatchResultRow<M>(vsr, vsmr));
			});
		});
		return resultRows;
	}
	
	@SafeVarargs
	@SuppressWarnings("unchecked")
	public static final <M extends VariationScannerMatchResult> TableColumn<VariationScanMatchResultRow<M>>[] generateResultColumns(
			Class<M> matchResultClass, 
			TableColumn<VariationScanResult<M>>... vsrColumns) {
		List<TableColumn<VariationScanMatchResultRow<M>>> columns = new ArrayList<TableColumn<VariationScanMatchResultRow<M>>>();
		if(matchResultClass != null) {
			for(TableColumn<VariationScanResult<M>> vsrColumn: vsrColumns) {
				columns.add(new TableColumn<VariationScanMatchResultRow<M>>(vsrColumn.getColumnHeader(), vsmrr -> vsrColumn.populateColumn(vsmrr.getVariationScanResult())));
			}
			List<TableColumn<? extends VariationScannerMatchResult>> vsmrColumns = VariationScannerMatchResult.getColumnsForMatchResultClass(matchResultClass);
			for(TableColumn<? extends VariationScannerMatchResult> vsmrColumn: vsmrColumns) {
				TableColumn<M> castVsmrColumn = (TableColumn<M>) vsmrColumn;
				columns.add(new TableColumn<VariationScanMatchResultRow<M>>(vsmrColumn.getColumnHeader(), vsmrr -> castVsmrColumn.populateColumn(vsmrr.getMatchResult())));
			}
		}
		return columns.toArray(new TableColumn[]{});
	}

	
}
