package uk.ac.gla.cvr.gluetools.core.collation.populating;

import java.util.List;
import java.util.regex.Pattern;

import uk.ac.gla.cvr.gluetools.core.collation.populating.regex.RegexExtractorFormatter;

public interface PropertyPopulator {

	public static final String DEFAULT_NULL_REGEX = " *";

	public RegexExtractorFormatter getMainExtractor();

	public List<RegexExtractorFormatter> getValueConverters();

	public Pattern getNullRegex();
	
	public default String getProperty() {
		return null;
	}
	
	public default boolean overwriteExistingNonNull() {
		return true;
	}

	public default boolean overwriteWithNewNull() {
		return false;
	}
}
