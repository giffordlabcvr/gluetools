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
	commandWords={"list", "almt-member"},
	docoptUsages={"[-w <whereClause>] [-p <pageSize>] [-l <fetchLimit>] [-o <fetchOffset>] [-s <sortProperties>] [<fieldName> ...]"},
	docoptOptions={
		"-w <whereClause>, --whereClause <whereClause>           Qualify result set",
		"-p <pageSize>, --pageSize <pageSize>                    Tune ORM page size",
		"-l <fetchLimit>, --fetchLimit <fetchLimit>              Limit max number of records",
		"-o <fetchOffset>, --fetchOffset <fetchOffset>           Record number offset",
		"-s <sortProperties>, --sortProperties <sortProperties>  Comma-separated sort properties"},
	description="List alignment members",
	furtherHelp=
	"The <pageSize> option is for performance tuning. The default page size\n"+
	"is 250 records.\n"+
	"The optional whereClause qualifies which sequences are displayed.\n"+
	"The optional sortProperties allows combined ascending/descending orderings, e.g. +property1,-property2.\n"+
	"Where fieldNames are specified, only these field values will be displayed.\n"+
	"Examples:\n"+
	"  list almt-member -w \"sequence.source.name = 'local'\"\n"+
	"  list almt-member -w \"sequence.custom_field = 'value1'\"\n"+
	"  list almt-member -w \"alignment.name = 'AL_MASTER'\" sequence.sequenceID sequence.custom_field"
) 
public class ListAlmtMemberCommand extends AbstractListCTableCommand {

	
	public ListAlmtMemberCommand() {
		super();
		setTableName(ConfigurableTable.alignment_member.name());
	}

	@CompleterClass
	public static final class Completer extends ListCommandCompleter {
		public Completer() {
			super(ConfigurableTable.alignment_member.name());
		}
	}


}
