package uk.ac.gla.cvr.gluetools.core.variationscanner;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CommandException.Code;
import uk.ac.gla.cvr.gluetools.core.command.result.TableColumn;
import uk.ac.gla.cvr.gluetools.core.plugins.Plugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;

public class VariationScanRenderHints implements Plugin {

	public static String SHOW_PATTERN_LOCS_SEPARATELY = "showPatternLocsSeparately";
	public static String SHOW_MATCH_VALUES_SEPARATELY = "showMatchValuesSeparately";
	public static String SHOW_MATCH_NT_LOCATIONS = "showMatchNtLocations";
	public static String SHOW_MATCH_LC_LOCATIONS = "showMatchLcLocations";
	
	
	// add a patternLocIndex column, and add a row for each patternLoc in the variation
	private boolean showPatternLocsSeparately;

	// add a matchedValue column, add add a row for each match (implies showPatternLocsSeparately)
	private boolean showMatchValuesSeparately;

	// (implies showMatchValuesSeparately)
	// add matchNtStart, matchNtEnd columns showing start / end locations for the match on the query.
	private boolean showMatchNtLocations;

	// (implies showMatchValuesSeparately)
	// add matchLcStart, matchLcEnd columns containing the labelled codon start / end locations for the match (if an amino acid variation).
	private boolean showMatchLcLocations;

	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		this.showPatternLocsSeparately = PluginUtils.configureBooleanProperty(configElem, SHOW_PATTERN_LOCS_SEPARATELY, true);
		this.showMatchValuesSeparately = PluginUtils.configureBooleanProperty(configElem, SHOW_MATCH_VALUES_SEPARATELY, true);
		this.showMatchNtLocations = PluginUtils.configureBooleanProperty(configElem, SHOW_MATCH_NT_LOCATIONS, true);
		this.showMatchLcLocations = PluginUtils.configureBooleanProperty(configElem, SHOW_MATCH_LC_LOCATIONS, true);
		
		if(showMatchValuesSeparately && !showPatternLocsSeparately) {
			throw new CommandException(Code.COMMAND_USAGE_ERROR, "showMatchValuesSeparately implies showPatternLocsSeparately");
		}
		if(showMatchNtLocations && !showMatchValuesSeparately) {
			throw new CommandException(Code.COMMAND_USAGE_ERROR, "showMatchNtLocations implies showMatchValuesSeparately");
		}
		if(showMatchLcLocations && !showMatchValuesSeparately) {
			throw new CommandException(Code.COMMAND_USAGE_ERROR, "showMatchLcLocations implies showMatchValuesSeparately");
		}
		
	}
	
	public List<VariationScanResultRow> scanResultsToResultRows(List<VariationScanResult> vsrs) {
		
		
		
		List<VariationScanResultRow> vsrrs = new ArrayList<VariationScanResultRow>();
		for(VariationScanResult vsr: vsrs) {
			if(!showPatternLocsSeparately) {
				vsrrs.add(new VariationScanResultRow(vsr));
			} else if(!showMatchValuesSeparately){
				vsrrs.addAll(vsr.getPLocScanResults()
						.stream()
						.map(plsr -> new VariationScanResultRow(vsr, plsr))
						.collect(Collectors.toList()));
			} else {
				for(PLocScanResult plsr: vsr.getPLocScanResults()) {
					for(int i = 0; i < plsr.getQueryLocs().size(); i++) {
						String lcStart = null, lcEnd = null;
						if(plsr instanceof AminoAcidPLocScanResult && showMatchLcLocations) {
							lcStart = ((AminoAcidPLocScanResult) plsr).getAaStartCodons().get(i);
							lcEnd = ((AminoAcidPLocScanResult) plsr).getAaEndCodons().get(i);
						}
						vsrrs.add(new VariationScanResultRow(vsr, plsr, plsr.getMatchedValues().get(i), plsr.getQueryLocs().get(i), lcStart, lcEnd));
					}
				}
			}
		}
		return vsrrs;
	}
	
	@SafeVarargs
	@SuppressWarnings("unchecked")
	public final TableColumn<VariationScanResultRow>[] generateResultColumns(TableColumn<VariationScanResult>... vsrColumns) {
		List<TableColumn<VariationScanResultRow>> columns = new ArrayList<TableColumn<VariationScanResultRow>>();
		for(TableColumn<VariationScanResult> vsrColumn: vsrColumns) {
			columns.add(new TableColumn<VariationScanResultRow>(vsrColumn.getColumnHeader(), vsrr -> vsrColumn.populateColumn(vsrr.getVsr())));
		}
		columns.add(new TableColumn<VariationScanResultRow>("present", vsrr -> vsrr.getVsr().isPresent()));
		if(showPatternLocsSeparately) {
			columns.add(new TableColumn<VariationScanResultRow>("pLocIndex", vsrr -> {
				PLocScanResult plsr = vsrr.getPlsr();
				return plsr == null ? null : plsr.getIndex();
			}));
			if(showMatchValuesSeparately){
				columns.add(new TableColumn<VariationScanResultRow>("matchedValue", vsrr -> vsrr.getMatchedValue()));
				if(showMatchNtLocations) {
					columns.add(new TableColumn<VariationScanResultRow>("queryNtStart", vsrr -> 
					{ 
						ReferenceSegment matchedValueSegment = vsrr.getMatchedValueSegment();
						return matchedValueSegment == null ? null : matchedValueSegment.getRefStart(); 
					}));
					columns.add(new TableColumn<VariationScanResultRow>("queryNtEnd", vsrr -> 
						{ 
							ReferenceSegment matchedValueSegment = vsrr.getMatchedValueSegment();
							return matchedValueSegment == null ? null : matchedValueSegment.getRefEnd(); 
						}));
				}
				if(showMatchLcLocations) {
					columns.add(new TableColumn<VariationScanResultRow>("lcStart", vsrr -> vsrr.getLcStart()));
					columns.add(new TableColumn<VariationScanResultRow>("lcEnd", vsrr -> vsrr.getLcEnd()));
				}
			} 
		}
		return columns.toArray(new TableColumn[]{});
	}
}