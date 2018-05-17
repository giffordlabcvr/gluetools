package uk.ac.gla.cvr.gluetools.core.variationscanner;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import uk.ac.gla.cvr.gluetools.core.command.result.TableColumn;
import uk.ac.gla.cvr.gluetools.core.document.CommandObject;

public abstract class VariationScannerMatchResult {

	protected VariationScannerMatchResult() {
		super();
	}

	public abstract int getRefStart();
	// used when scanning NGS data, the worst qscore of any base contributing to the match.
	private Integer worstContributingQScore;

	
	// we don't want to do too much reflection!
	private static Map<Class<? extends VariationScannerMatchResult>, List<TableColumn<? extends VariationScannerMatchResult>>> columnCache
		= new LinkedHashMap<Class<? extends VariationScannerMatchResult>, List<TableColumn<? extends VariationScannerMatchResult>>>();
	
	public static <D> TableColumn<D> column(String header, Function<D, Object> columnPopulator) {
		return new TableColumn<D>(header, columnPopulator);
	}

	// override this to implement document representation of match result, not based on table columns.
	@SuppressWarnings("unchecked")
	public <M extends VariationScannerMatchResult> void populateMatchObject(CommandObject matchObject) {
		M matchResult = (M) this;
		List<TableColumn<? extends VariationScannerMatchResult>> tableColumns = getColumnsForMatchResultClass(matchResult.getClass());
		for(TableColumn<? extends VariationScannerMatchResult> tableColumn: tableColumns) {
			TableColumn<M> castColumn = (TableColumn<M>) tableColumn;
			String name = castColumn.getColumnHeader();
			Object value = castColumn.getColumnPopulator().apply(matchResult);
			matchObject.set(name, value);
		}
	}

	@SuppressWarnings("unchecked")
	public synchronized static List<TableColumn<? extends VariationScannerMatchResult>> getColumnsForMatchResultClass(
		Class<? extends VariationScannerMatchResult> matchResultClass) {
		List<TableColumn<? extends VariationScannerMatchResult>> cachedColumns = 
				(List<TableColumn<? extends VariationScannerMatchResult>>) columnCache.get(matchResultClass);
		if(cachedColumns != null) {
			return cachedColumns;
		}
		Method getTableColumnsMethod = null;
		try {
			getTableColumnsMethod = matchResultClass.getDeclaredMethod("getTableColumns");
		} catch (ReflectiveOperationException roe) {
			throw new RuntimeException("Could not find getTableColumns method in class "+matchResultClass.getSimpleName(), roe);
		}
		List<TableColumn<? extends VariationScannerMatchResult>> vsmrColumns;
		try {
			vsmrColumns = (List<TableColumn<? extends VariationScannerMatchResult>>) getTableColumnsMethod.invoke(null);
		} catch (ReflectiveOperationException roe) {
			throw new RuntimeException("Failed to invoke getTableColumns method in class "+matchResultClass.getSimpleName(), roe);
		}
		columnCache.put(matchResultClass, vsmrColumns);
		return vsmrColumns;
	}

	public Integer getWorstContributingQScore() {
		return worstContributingQScore;
	}

	public void setWorstContributingQScore(Integer worstContributingQScore) {
		this.worstContributingQScore = worstContributingQScore;
	}

}
