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
package uk.ac.gla.cvr.gluetools.core.collation.populating.genbank;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;

@CommandClass( 
		commandWords={"update", "custom-tables"}, 
		docoptUsages={"[-b <batchSize>] [(-p | -s)] [-w <whereClause>]"},
		docoptOptions={
				"-w <whereClause>, --whereClause <whereClause>  Qualify updated sequences",
				"-b <batchSize>, --batchSize <batchSize>        Commit batch size [default: 250]",
				"-p, --preview                                  Database will not be updated",
				"-s, --silent                                   No result table"
		},
		metaTags = {},
		description="Create custom table rows based on Genbank XML",
		furtherHelp=
		"The <batchSize> argument allows you to control how often updates are committed to the database "+
				"during the import. The default is every 250 sequences. A larger <batchSize> means fewer database "+
				"accesses, but requires more Java heap memory. ") 
public class GenbankXmlPopulatorUpdateCustomTablesCommand extends GenbankXmlPopulatorCommand {

	@Override
	protected CommandResult execute(CommandContext cmdContext, GenbankXmlPopulator populatorPlugin) {
		return populatorPlugin.updateCustomTables(cmdContext, getBatchSize(), getWhereClause(), !getPreview(), getSilent());
	}
	
	@CompleterClass
	public static class Completer extends ModuleCmdCompleter<GenbankXmlPopulator> {
		public Completer() {
			super();
		}
	}
	
}
