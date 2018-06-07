package uk.ac.gla.cvr.gluetools.core.collation.populating.regex;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import uk.ac.gla.cvr.gluetools.utils.IsoCountry;
import uk.ac.gla.cvr.gluetools.utils.IsoCountryUtils.CodeStyle;

public class IsoCountryMatcherConverter implements MatcherConverter {

	private CodeStyle codeStyle;
	
	public IsoCountryMatcherConverter(CodeStyle codeStyle) {
		super();
		this.codeStyle = codeStyle;
	}

	@Override
	public String matchAndConvert(String input) {
		IsoCountry bestCountry = null;
		Integer bestMatchPosition = null;
		Integer bestMatchLength = null;
		for(IsoCountry isoCountry: IsoCountry.values()) {
			for(Pattern pattern: isoCountry.getPatterns()) {
				Matcher matcher = pattern.matcher(input);
				boolean found = matcher.find();
				if(found) {
					int matchStart = matcher.start();
					int matchLength = matcher.end() - matchStart;
					if(bestCountry == null || 
							matchStart < bestMatchPosition ||
							(matchStart == bestMatchPosition && matchLength > bestMatchLength)) {
						bestCountry = isoCountry;
						bestMatchPosition = matchStart;
						bestMatchLength = matchLength;
					}
				} 
			}
		}
		if(bestCountry == null) {
			return null;
		}
		String output;
		switch(codeStyle) {
		case OFFICIAL:
			output = bestCountry.getOfficialName();
			break;
		case SHORT:
			output = bestCountry.getShortName();
			break;
		case ALPHA_2:
			output = bestCountry.getAlpha2();
			break;
		case ALPHA_3:
			output = bestCountry.getAlpha3();
			break;
		case NUMERIC:
			output = bestCountry.getNumeric();
			break;
		default:
			throw new RuntimeException("Unable to handle codeStyle "+codeStyle);
		}
		return output;
	}

}
