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
package uk.ac.gla.cvr.gluetools.core.command.console;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.OkResult;

@CommandClass(
		commandWords={"commit"},
		docoptUsages={""},
		docoptOptions={},
		description="Commit any uncommitted changes to the database",
		furtherHelp="Most commands which update the database will commit these changes immediately. "+
		"However, some have an option to suppress the commit, via a -C or --noCommit option. This command can "+
		"be used subsequently to flush these changes to the database. "+
		"The main use of this is for efficiency: a large number of non-committed changes can be batched up and committed "+
		"together.",
		metaTags = { CmdMeta.updatesDatabase }
	) 
public class CommitCommand extends Command<OkResult> {

	@Override
	public OkResult execute(CommandContext cmdContext) {
		cmdContext.commit();
		return new OkResult();
	}

}
