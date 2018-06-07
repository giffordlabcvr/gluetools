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
package uk.ac.gla.cvr.gluetools.core.collation.populating;

import java.util.List;
import java.util.regex.Pattern;

import uk.ac.gla.cvr.gluetools.core.collation.populating.regex.MatcherConverter;

public interface ValueExtractor {

	public static final String DEFAULT_NULL_REGEX = " *";

	public MatcherConverter getMainExtractor();

	public List<? extends MatcherConverter> getValueConverters();

	public Pattern getNullRegex();

	public static String extractAndConvert(String input, MatcherConverter mainExtractor, 
			List<? extends MatcherConverter> valueConverters) {
		if(mainExtractor != null) {
			String mainExtractorResult = mainExtractor.matchAndConvert(input);
			if(mainExtractorResult == null) {
				return null;
			} else {
				input = mainExtractorResult;
			}
		}
		if(valueConverters != null) {
			for(MatcherConverter valueConverter: valueConverters) {
				String valueConverterResult = valueConverter.matchAndConvert(input);
				if(valueConverterResult != null) {
					return valueConverterResult;
				}
			}
		}
		return input;
	}

	public static String extractValue(ValueExtractor valueExtractor, String inputText) {
		String extractAndConvertResult = 
				ValueExtractor.extractAndConvert(inputText, valueExtractor.getMainExtractor(), valueExtractor.getValueConverters());
		if(extractAndConvertResult != null) {
			Pattern nullRegex = valueExtractor.getNullRegex();
			if(nullRegex == null || !nullRegex.matcher(extractAndConvertResult).matches()) {
				return extractAndConvertResult;
			}
		}
		return null;
	}
}
