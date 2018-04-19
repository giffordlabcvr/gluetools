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

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.Variation;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.VariationException;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.VariationException.Code;
import uk.ac.gla.cvr.gluetools.core.plugins.Plugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

public class VariationScanRenderHints implements Plugin {

	public static String SHOW_MATCHES_AS_TABLE = "showMatchesAsTable";
	public static String SHOW_MATCHES_AS_DOCUMENT = "showMatchesAsDocument";
	
	// table with a row for each match
	private boolean showMatchesAsTable;

	// document with an object for each match
	private boolean showMatchesAsDocument;

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		this.showMatchesAsTable = PluginUtils.configureBooleanProperty(configElem, SHOW_MATCHES_AS_TABLE, true);
		this.showMatchesAsDocument = PluginUtils.configureBooleanProperty(configElem, SHOW_MATCHES_AS_DOCUMENT, true);
		if(showMatchesAsTable && showMatchesAsDocument) {
			throw new CommandException(CommandException.Code.COMMAND_USAGE_ERROR, "Only one of --showMatchesAsTable and --showMatchesAsDocument may be used");
		}
	}
	
	public boolean showMatchesAsTable() {
		return showMatchesAsTable;
	}

	public boolean showMatchesAsDocument() {
		return showMatchesAsDocument;
	}

	public static Class<? extends VariationScannerMatchResult> getMatchResultClass(List<Variation> variations) {
		Variation.VariationType type = null;
		for(Variation variation: variations) {
			if(type == null) {
				type = variation.getVariationType();
			} else {
				if(variation.getVariationType() != type) {
					throw new VariationException(Code.VARIATIONS_OF_DIFFERENT_TYPES);
				}
			}
		}
		return type.getMatchResultClass();
	}
	
}
