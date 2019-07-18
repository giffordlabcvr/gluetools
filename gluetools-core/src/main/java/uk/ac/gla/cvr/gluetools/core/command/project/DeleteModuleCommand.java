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

import java.util.List;
import java.util.Optional;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.query.SelectQuery;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.result.DeleteResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.module.Module;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass( 
	commandWords={"delete", "module"}, 
	metaTags = {},
	docoptUsages={"(<moduleName> | -w <whereClause>)"},
	docoptOptions={
	"-w <whereClause>, --whereClause <whereClause>  Qualify which modules should be deleted"},
	description="Delete a module") 
public class DeleteModuleCommand extends ProjectModeCommand<DeleteResult> {

	private String moduleName;
	private Optional<Expression> whereClause;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		moduleName = PluginUtils.configureStringProperty(configElem, "moduleName", false);
		whereClause = Optional.ofNullable(PluginUtils.configureCayenneExpressionProperty(configElem, "whereClause", false));
		if( !(
				(moduleName != null && !whereClause.isPresent()) || 
				(moduleName == null && whereClause.isPresent())
			) 
		) {
			usageError();
		}
		
	}

	private void usageError() {
		throw new CommandException(CommandException.Code.COMMAND_USAGE_ERROR, "Either whereClause or moduleName must be specified but not both");
	}

	@Override
	public DeleteResult execute(CommandContext cmdContext) {
		DeleteResult result;
		if(moduleName != null) {
			result = GlueDataObject.delete(cmdContext, Module.class, Module.pkMap(moduleName), true);
		} else {
			List<Module> modulesToDelete = GlueDataObject.query(cmdContext, Module.class, new SelectQuery(Module.class, whereClause.get()));
			int numModules = modulesToDelete.size();
			modulesToDelete.forEach(mod -> {
				GlueDataObject.delete(cmdContext, Module.class, mod.pkMap(), false);
			});
			result = new DeleteResult(Module.class, numModules);
		}
		cmdContext.commit();
		return result;
	}

	@CompleterClass
	public static class Completer extends ModuleNameCompleter {}

}
