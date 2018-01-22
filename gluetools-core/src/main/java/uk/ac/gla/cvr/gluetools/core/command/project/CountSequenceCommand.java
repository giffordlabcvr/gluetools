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
package uk.ac.gla.cvr.gluetools.core.command.project;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ConfigurableTable;


@CommandClass( 
	commandWords={"count", "sequence"},
	docoptUsages={"[-w <whereClause>]"},
	docoptOptions={
		"-w <whereClause>, --whereClause <whereClause>           Qualify result set"},
	description="Count sequences",
	furtherHelp=
	"The optional whereClause qualifies which sequences are included.\n"+
	"Examples:\n"+
	"  count sequence -w \"source.name = 'local'\"\n"+
	"  count sequence -w \"sequenceID like 'f%' and custom_field = 'value1'\""
) 
public class CountSequenceCommand extends AbstractCountCTableCommand {

	
	public CountSequenceCommand() {
		super();
		setTableName(ConfigurableTable.sequence.name());
	}

	@CompleterClass
	public static final class Completer extends CountCommandCompleter {
		public Completer() {
			super(ConfigurableTable.sequence.name());
		}
	}


}
