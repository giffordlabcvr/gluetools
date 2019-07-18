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
package uk.ac.gla.cvr.gluetools.core.command.configurableobject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.CompletionSuggestion;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.UpdateResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.link.Link;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;


@CommandClass( 
	commandWords={"remove", "link-target"}, 
	docoptUsages={"[-C] <linkName> <targetPath>"},
	docoptOptions={
			"-C, --noCommit     Don't commit to the database [default: false]",
	},
	metaTags = {},
	description="Remove a target object on a link") 
public class ConfigurableObjectRemoveLinkTargetCommand extends Command<UpdateResult> {

	public static final String LINK_NAME = PropertyCommandDelegate.LINK_NAME;
	public static final String TARGET_PATH = PropertyCommandDelegate.TARGET_PATH;
	public static final String NO_COMMIT = PropertyCommandDelegate.NO_COMMIT;

	
	private PropertyCommandDelegate propertyCommandDelegate = new PropertyCommandDelegate();
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		propertyCommandDelegate.configureRemoveLinkTarget(pluginConfigContext, configElem);
	}

	@Override
	public UpdateResult execute(CommandContext cmdContext) {
		return propertyCommandDelegate.executeRemoveLinkTarget(cmdContext);
	}

	@CompleterClass
	public static class Completer extends PropertyCommandDelegate.ToManyLinkNameCompleter {
		public Completer() {
			super();
			registerVariableInstantiator("targetPath", new VariableInstantiator() {
				@SuppressWarnings("unchecked")
				@Override
				public List<CompletionSuggestion> instantiate(
						ConsoleCommandContext cmdContext,
						@SuppressWarnings("rawtypes") Class<? extends Command> cmdClass, Map<String, Object> bindings,
						String prefix) {
					ConfigurableObjectMode configurableObjectMode = (ConfigurableObjectMode) cmdContext.peekCommandMode();
					String linkName = (String) bindings.get("linkName");
					if(linkName != null) {
						Project project = configurableObjectMode.getProject();
						String tableName = configurableObjectMode.getTableName();
						GlueDataObject thisObject = configurableObjectMode.getConfigurableObject(cmdContext);
						Link link = project.getSrcTableLink(tableName, linkName);
						if(link != null && link.isToMany()) {
							String destTableName = link.getDestTableName();
							List<? extends GlueDataObject> currentTargets = (List<? extends GlueDataObject>) thisObject.readProperty(linkName);
							return currentTargets.stream()
									.map(t -> new CompletionSuggestion(project.pkMapToTargetPath(destTableName, t.pkMap()), true))
									.collect(Collectors.toList());
						}
						link = project.getDestTableLink(tableName, linkName);
						if(link != null && link.isFromMany()) {
							String srcTableName = link.getSrcTableName();
							List<? extends GlueDataObject> currentTargets = (List<? extends GlueDataObject>) thisObject.readProperty(linkName);
							return currentTargets.stream()
									.map(t -> new CompletionSuggestion(project.pkMapToTargetPath(srcTableName, t.pkMap()), true))
									.collect(Collectors.toList());
						}
					}
					return new ArrayList<CompletionSuggestion>();
				}

			});

		}
	}

}
