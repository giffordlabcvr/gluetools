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
package uk.ac.gla.cvr.gluetools.core.command.dataUtils;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.IsoCountry;


@CommandClass(
		commandWords={"data-util", "parse-country"}, 
		description = "Parse a country string to its ISO equivalent", 
		docoptUsages = { "<countryString>" },
		docoptOptions = {},
		furtherHelp = ""
)
public class DataUtilParseCountryCommand extends Command<DataUtilParseCountryResult> {

	public static final String COUNTRY_STRING = "countryString";

	private String countryString;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.countryString = PluginUtils.configureStringProperty(configElem, COUNTRY_STRING, true);
	}

	@Override
	public DataUtilParseCountryResult execute(CommandContext cmdContext) {
		return new DataUtilParseCountryResult(IsoCountry.parseCountry(countryString));
	}

} 