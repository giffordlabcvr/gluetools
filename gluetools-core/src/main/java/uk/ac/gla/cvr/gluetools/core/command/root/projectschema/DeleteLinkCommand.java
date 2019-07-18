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
package uk.ac.gla.cvr.gluetools.core.command.root.projectschema;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.CompletionSuggestion;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.InsideProjectMode;
import uk.ac.gla.cvr.gluetools.core.command.result.DeleteResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ModelBuilder;
import uk.ac.gla.cvr.gluetools.core.datamodel.link.Link;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@CommandClass( 
		commandWords={"delete", "link"}, 
		docoptUsages={"<srcTableName> <srcLinkName>"},
		description="Delete a custom relational link") 
public class DeleteLinkCommand extends ProjectSchemaModeCommand<DeleteResult> {

	public static final String SRC_TABLE_NAME = "srcTableName";
	public static final String SRC_LINK_NAME = "srcLinkName";

	
	private String srcTableName;
	private String srcLinkName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.srcTableName = PluginUtils.configureStringProperty(configElem, SRC_TABLE_NAME, true);
		this.srcLinkName = PluginUtils.configureIdentifierProperty(configElem, SRC_LINK_NAME, true);
	}

	@Override
	public DeleteResult execute(CommandContext cmdContext) {
		Link link = GlueDataObject.lookup(cmdContext, Link.class, Link.pkMap(getProjectName(), srcTableName, srcLinkName), true);
		if(link != null) {
			deleteLink(cmdContext, link);
			cmdContext.commit();
			return new DeleteResult(Link.class, 1);
		} else {
			return new DeleteResult(Link.class, 0);
		}
	}

	public static void deleteLink(CommandContext cmdContext, Link link) {
		ModelBuilder.deleteLinkFromModel(cmdContext.getGluetoolsEngine(), link.getProject(), link);
		GlueDataObject.delete(cmdContext, Link.class, link.pkMap(), false);
	}

	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerVariableInstantiator("srcTableName", new VariableInstantiator() {
				@Override
				public List<CompletionSuggestion> instantiate(
						ConsoleCommandContext cmdContext,
						@SuppressWarnings("rawtypes") Class<? extends Command> cmdClass, Map<String, Object> bindings,
						String prefix) {
					InsideProjectMode insideProjectMode = (ProjectSchemaMode) cmdContext.peekCommandMode();
					return insideProjectMode.getProject().getLinks()
							.stream()
							.map(l -> new CompletionSuggestion(l.getSrcTableName(), true))
							.collect(Collectors.toList());
				}
			});
			registerVariableInstantiator("srcLinkName", new VariableInstantiator() {
				@Override
				public List<CompletionSuggestion> instantiate(
						ConsoleCommandContext cmdContext,
						@SuppressWarnings("rawtypes") Class<? extends Command> cmdClass, Map<String, Object> bindings,
						String prefix) {
					String srcTableName = (String) bindings.get("srcTableName");
					if(srcTableName != null) {
						InsideProjectMode insideProjectMode = (ProjectSchemaMode) cmdContext.peekCommandMode();
						return insideProjectMode.getProject().getLinks()
								.stream()
								.filter(l -> l.getSrcTableName().equals(srcTableName))
								.map(l -> new CompletionSuggestion(l.getSrcLinkName(), true))
								.collect(Collectors.toList());
					} else {
						return new ArrayList<CompletionSuggestion>();
					}
				}
			});
		}
	}


}

