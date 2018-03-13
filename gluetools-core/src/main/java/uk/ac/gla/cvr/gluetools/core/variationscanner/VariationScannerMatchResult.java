package uk.ac.gla.cvr.gluetools.core.variationscanner;

import java.util.function.Function;

import uk.ac.gla.cvr.gluetools.core.command.result.TableColumn;

public abstract class VariationScannerMatchResult {

	public abstract int getRefStart();
	
	public static <D> TableColumn<D> column(String header, Function<D, Object> columnPopulator) {
		return new TableColumn<D>(header, columnPopulator);
	}

}
