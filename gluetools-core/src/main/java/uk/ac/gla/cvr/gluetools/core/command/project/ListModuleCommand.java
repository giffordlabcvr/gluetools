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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.query.SelectQuery;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.CompletionSuggestion;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.ListResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.module.Module;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePluginFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass( 
	commandWords={"list", "module"}, 
	docoptUsages={"[-t <moduleType>] [-w <whereClause>]"},
	docoptOptions = {
	"-t <moduleType>, --moduleType <moduleType>     List modules of a specific type",
	"-w <whereClause>, --whereClause <whereClause>  Qualify listed modules"},
	description="List modules") 
public class ListModuleCommand extends ProjectModeCommand<ListResult> {

	private String moduleType;
	private Optional<Expression> whereClause;
	

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.moduleType = PluginUtils.configureStringProperty(configElem, "moduleType", false);
		this.whereClause = Optional.ofNullable(PluginUtils.configureCayenneExpressionProperty(configElem, "whereClause", false));
	}
	
	@Override
	public ListResult execute(CommandContext cmdContext) {
		List<Module> modules = listModules(cmdContext, whereClause, moduleType);
		
		return new ListResult(cmdContext, Module.class, modules, Arrays.asList("name", "type"), 
				new BiFunction<Module, String, Object>() {
					@Override
					public Object apply(Module t, String u) {
						if(u.equals("name")) {
							return t.getName();
						} else if(u.equals("type")) {
							return t.getType();
						}
						return null;
					}
		});
	}

	public static List<Module> listModules(CommandContext cmdContext, String moduleType) {
		return listModules(cmdContext, Optional.empty(), moduleType);
	}
	
	public static List<Module> listModules(CommandContext cmdContext, Optional<Expression> whereClause, String moduleType) {
		SelectQuery query;
		if(whereClause.isPresent()) {
			query = new SelectQuery(Module.class, whereClause.get());
		} else {
			query = new SelectQuery(Module.class);
		}
		List<Module> modules = GlueDataObject.query(cmdContext, Module.class, query);
		if(moduleType != null) {
			modules = modules.stream().filter(m -> m.getType().equals(moduleType)).collect(Collectors.toList());
		}
		return modules;
	}

	@CompleterClass
	public static final class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerVariableInstantiator("moduleType", new VariableInstantiator() {
				@SuppressWarnings("rawtypes")
				@Override
				public List<CompletionSuggestion> instantiate(
						ConsoleCommandContext cmdContext,
						Class<? extends Command> cmdClass, Map<String, Object> bindings,
						String prefix) {
					ModulePluginFactory pluginFactory = PluginFactory.get(ModulePluginFactory.creator);
					return pluginFactory.getElementNames().stream()
							.map(eName -> new CompletionSuggestion(eName, true))
							.collect(Collectors.toList());
				}
			});
		}
		
	}
	
}
