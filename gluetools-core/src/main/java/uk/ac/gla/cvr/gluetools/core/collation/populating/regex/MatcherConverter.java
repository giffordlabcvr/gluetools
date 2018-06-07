package uk.ac.gla.cvr.gluetools.core.collation.populating.regex;

import uk.ac.gla.cvr.gluetools.core.plugins.Plugin;

public interface MatcherConverter extends Plugin {

	public String matchAndConvert(String input);
}
