/**
 *    GLUE: A flexible system for virus sequence data
 *    Copyright (C) 2018 The University of Glasgow
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Affero General Public License as published
 *    by the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Affero General Public License for more details.

 *    You should have received a copy of the GNU Affero General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *    Contact details:
 *    MRC-University of Glasgow Centre for Virus Research
 *    Sir Michael Stoker Building, Garscube Campus, 464 Bearsden Road, 
 *    Glasgow G61 1QH, United Kingdom
 *    
 *    Josh Singer: josh.singer@glasgow.ac.uk
 *    Rob Gifford: robert.gifford@glasgow.ac.uk
*/
package uk.ac.gla.cvr.gluetools.core.variationscanner;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.Variation;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.VariationException;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.VariationException.Code;
import uk.ac.gla.cvr.gluetools.core.datamodel.variationMetatag.VariationMetatag.VariationMetatagType;
import uk.ac.gla.cvr.gluetools.core.segments.NtQueryAlignedSegment;

public abstract class BaseVariationScanner<M extends VariationScannerMatchResult> {
	
	private List<VariationMetatagType> allowedMetatagTypes;
	private List<VariationMetatagType> requiredMetatagTypes;
	private Variation variation;
	private Map<VariationMetatagType, String> metatagsMap;
	
	protected BaseVariationScanner(List<VariationMetatagType> allowedMetatagTypes, List<VariationMetatagType> requiredMetatagTypes) {
		super();
		this.allowedMetatagTypes = allowedMetatagTypes;
		this.requiredMetatagTypes = requiredMetatagTypes;
		for(VariationMetatagType requiredMetatagType: requiredMetatagTypes) {
			if(!allowedMetatagTypes.contains(requiredMetatagType)) {
				throw new RuntimeException("Required metatag type must also be listed as allowed.");
			}
		}
	}
	
	public final void init(CommandContext cmdContext, Variation variation) {
		this.variation = variation;
		this.metatagsMap = variation.getMetatagsMap();
		this.init(cmdContext);
	}
	
	protected void init(CommandContext cmdContext) {}

	protected Variation getVariation() {
		return variation;
	}

	protected Map<VariationMetatagType, String> getMetatagsMap() {
		return metatagsMap;
	}

	public void validate() {
		for(VariationMetatagType requiredMetatagType: requiredMetatagTypes) {
			if(!metatagsMap.containsKey(requiredMetatagType)) {
				throwScannerException("Missing required metatag with key: "+requiredMetatagType.name());
			}
		}
		for(VariationMetatagType metatagType: metatagsMap.keySet()) {
			if(!allowedMetatagTypes.contains(metatagType)) {
				throwScannerException("Metatag key: "+metatagType.name()+" is not allowed for variation type "+variation.getVariationType().name());
			}
		}
	}

	public void throwScannerException(String errorTxt) {
		throw new VariationException(Code.VARIATION_SCANNER_EXCEPTION, variation.getFeatureLoc().getReferenceSequence().getName(), 
				variation.getFeatureLoc().getFeature().getName(), 
				variation.getName(), errorTxt);
	}

	public List<VariationMetatagType> getAllowedMetatagTypes() {
		return allowedMetatagTypes;
	}
	
	public abstract VariationScanResult<M> scan(CommandContext cmdContext, List<NtQueryAlignedSegment> queryToRefNtSegs);

	protected Pattern parseRegex(String stringPattern) {
		try {
			return Pattern.compile(stringPattern);
		} catch(PatternSyntaxException pse) {
			throwScannerException("Syntax error in variation regex: "+pse.getMessage());
			return null; // unreachable!
		}
	}

	// could be overridden by specific scanner types, perhaps based on metatag settings.
	protected abstract boolean computeSufficientCoverage(List<NtQueryAlignedSegment> queryToRefNtSegs);

	protected Integer getIntMetatagValue(VariationMetatagType type) {
		String stringValue = getMetatagsMap().get(type);
		if(stringValue != null) {
			try {
				return Integer.parseInt(stringValue);
			} catch(NumberFormatException nfe) {
				throwScannerException("Expected integer value for metatag: "+type.name());
			}
		}
		return null;
	}
}
