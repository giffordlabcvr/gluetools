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
package uk.ac.gla.cvr.gluetools.core.digs.importer;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter.VariableInstantiator;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CompletionSuggestion;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModulePluginCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ProvidedProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;

public abstract class DigsImporterCommand<R extends CommandResult> extends ModulePluginCommand<R, DigsImporter> 
	implements ProvidedProjectModeCommand {

	protected static class DigsDbNameInstantiator extends VariableInstantiator {
		@SuppressWarnings("rawtypes")
		@Override
		public List<CompletionSuggestion> instantiate(
				ConsoleCommandContext cmdContext,
				Class<? extends Command> cmdClass,
				Map<String, Object> bindings, String prefix) {
			try {
				return DigsImporter.listDigsDatabases(cmdContext).stream()
						.map(dbName -> new CompletionSuggestion(dbName, true))
						.collect(Collectors.toList());
			} catch(Exception e) {
				return null;
			}
		}
		
	}
	
}
