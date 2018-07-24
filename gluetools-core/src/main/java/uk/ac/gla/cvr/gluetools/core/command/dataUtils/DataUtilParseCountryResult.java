package uk.ac.gla.cvr.gluetools.core.command.dataUtils;

import uk.ac.gla.cvr.gluetools.core.command.result.MapResult;
import uk.ac.gla.cvr.gluetools.utils.IsoCountry;

public class DataUtilParseCountryResult extends MapResult {

	public DataUtilParseCountryResult(IsoCountry country) {
		super("dataUtilParseCountryResult", 
				mapBuilder()
				.put("parseSucceeded", country != null)
				.put("isoAlpha2", country == null ? null : country.getAlpha2())
				.put("isoAlpha3", country == null ? null : country.getAlpha3()) 
				.put("isoNumeric", country == null ? null : country.getNumeric())
				.put("officialName", country == null ? null : country.getOfficialName())
				.put("shortName", country == null ? null : country.getShortName())
		);
	}

}
