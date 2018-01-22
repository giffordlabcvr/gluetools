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
package uk.ac.gla.cvr.gluetools.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import uk.ac.gla.cvr.gluetools.core.collation.populating.regex.RegexExtractorFormatter;

public class IsoCountryUtils {

	public enum CodeStyle {
		OFFICIAL,
		SHORT,
		ALPHA_2,
		ALPHA_3,
		NUMERIC;
	}

	public static List<RegexExtractorFormatter> isoCountryValueConverters(CodeStyle codeStyle) {
		List<RegexExtractorFormatter> formatters = new ArrayList<RegexExtractorFormatter>();
		for(IsoCountry isoCountry: IsoCountry.values()) {
			RegexExtractorFormatter formatter = new RegexExtractorFormatter();
			formatter.setMatchPatterns(Arrays.asList(isoCountry.getPatterns()));
			String output = null;
			switch(codeStyle) {
			case OFFICIAL:
				output = isoCountry.getOfficialName();
				break;
			case SHORT:
				output = isoCountry.getShortName();
				break;
			case ALPHA_2:
				output = isoCountry.getAlpha2();
				break;
			case ALPHA_3:
				output = isoCountry.getAlpha3();
				break;
			case NUMERIC:
				output = isoCountry.getNumeric();
				break;
			default:
				throw new RuntimeException("Unable to handle codeStyle "+codeStyle);
			}
			formatter.setOutputString(output);
			formatters.add(formatter);
		}
		return formatters;
	}
	
}
