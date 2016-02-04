package uk.ac.gla.cvr.gluetools.core.digs.importer;

import java.util.List;
import java.util.function.BiFunction;

import uk.ac.gla.cvr.gluetools.core.command.result.ListResult;
import uk.ac.gla.cvr.gluetools.core.digs.importer.model.Extracted;

public class ListExtractedResult extends ListResult {

	public ListExtractedResult(List<Extracted> extracteds, List<String> fieldNames) {
		super(Extracted.class, extracteds, fieldNames, new BiFunction<Extracted, String, Object>() {
			@Override
			public Object apply(Extracted extracted, String propertyName) {
				return extracted.readNestedProperty(propertyName);
			}
		});
	}

}
