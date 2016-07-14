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
