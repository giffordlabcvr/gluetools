package uk.ac.gla.cvr.gluetools.core.command.project.alignment.member;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import uk.ac.gla.cvr.gluetools.core.command.result.BaseTableResult;
import uk.ac.gla.cvr.gluetools.core.command.result.TableColumn;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.Variation;
import uk.ac.gla.cvr.gluetools.core.variationscanner.VariationScanMatchResultRow;
import uk.ac.gla.cvr.gluetools.core.variationscanner.VariationScanResult;
import uk.ac.gla.cvr.gluetools.core.variationscanner.VariationScannerMatchResult;

public class VariationScanMatchCommandResult<M extends VariationScannerMatchResult> extends BaseTableResult<VariationScanMatchResultRow<M>> {

	public VariationScanMatchCommandResult(Class<M> matchResultClass, List<VariationScanResult<M>> vsrs) {
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
		for(TableColumn<VariationScanResult<M>> vsrColumn: vsrColumns) {
			columns.add(new TableColumn<VariationScanMatchResultRow<M>>(vsrColumn.getColumnHeader(), vsmrr -> vsrColumn.populateColumn(vsmrr.getVariationScanResult())));
		}
		Method getTableColumnsMethod = null;
		try {
			getTableColumnsMethod = matchResultClass.getDeclaredMethod("getTableColumns");
		} catch (ReflectiveOperationException roe) {
			throw new RuntimeException("Could not find getTableColumns method in class "+matchResultClass.getSimpleName(), roe);
		}
		List<TableColumn<M>> vsmrColumns;
		try {
			vsmrColumns = (List<TableColumn<M>>) getTableColumnsMethod.invoke(null);
		} catch (ReflectiveOperationException roe) {
			throw new RuntimeException("Failed to invoke getTableColumns method in class "+matchResultClass.getSimpleName(), roe);
		}
		for(TableColumn<M> vsmrColumn: vsmrColumns) {
			columns.add(new TableColumn<VariationScanMatchResultRow<M>>(vsmrColumn.getColumnHeader(), vsmrr -> vsmrColumn.populateColumn(vsmrr.getMatchResult())));
		}
		return columns.toArray(new TableColumn[]{});
	}

	
}
