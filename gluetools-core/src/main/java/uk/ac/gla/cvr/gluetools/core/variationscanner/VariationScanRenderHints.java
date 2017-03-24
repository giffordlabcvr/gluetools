package uk.ac.gla.cvr.gluetools.core.variationscanner;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.plugins.Plugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

public class VariationScanRenderHints implements Plugin {

	public static String SHOW_PRESENCE = "showPresence";
	public static String SHOW_PATTERN_LOCS_SEPARATELY = "showPatternLocsSeparately";
	public static String SHOW_MATCH_VALUES_SEPARATELY = "showMatchValuesSeparately";
	public static String SHOW_MATCH_CODON_LOCATIONS = "showMatchCodonLocations";
	public static String SHOW_MATCH_NT_LOCATIONS = "showMatchNtLocations";
	
	
	// add a column "present" which is true if the scan was possible and produced a match,
	// false if it was possible but did not match.
	private boolean showPresence;
	
	// add a patternLocIndex column, and add a row for each patternLoc in the variation
	private boolean showPatternLocsSeparately;

	// add a matchedValue column, add add a row for each match (implies showPatternLocsSeparately)
	private boolean showMatchValuesSeparately;

	// add matchLcStart, matchLcEnd columns, add add a row for each match (implies showMatchValuesSeparately)
	// containing the labelled codon start / end locations for the match.
	private boolean showMatchCodonLocations;

	// add matchNtStart, matchNtEnd columns, add add a row for each match (implies showMatchValuesSeparately)
	// containing the labelled codon start / end locations for the match.
	private boolean showMatchNtLocations;

	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		this.showPresence = PluginUtils.configureBooleanProperty(configElem, SHOW_PRESENCE, true);
		this.showPatternLocsSeparately = PluginUtils.configureBooleanProperty(configElem, SHOW_PATTERN_LOCS_SEPARATELY, true);
		this.showMatchValuesSeparately = PluginUtils.configureBooleanProperty(configElem, SHOW_MATCH_VALUES_SEPARATELY, true);
		this.showMatchCodonLocations = PluginUtils.configureBooleanProperty(configElem, SHOW_MATCH_CODON_LOCATIONS, true);
		this.showMatchNtLocations = PluginUtils.configureBooleanProperty(configElem, SHOW_MATCH_NT_LOCATIONS, true);
		
	}

}
