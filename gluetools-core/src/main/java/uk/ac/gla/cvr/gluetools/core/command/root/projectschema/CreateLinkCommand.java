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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CommandException.Code;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.CompletionSuggestion;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.InsideProjectMode;
import uk.ac.gla.cvr.gluetools.core.command.result.CreateResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ModelBuilder;
import uk.ac.gla.cvr.gluetools.core.datamodel.link.Link;
import uk.ac.gla.cvr.gluetools.core.datamodel.link.Link.Multiplicity;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@CommandClass( 
		commandWords={"create", "link"}, 
		docoptUsages={"<srcTableName> [-s <srcLinkName>] <destTableName> [-d <destLinkName>] [-m <multiplicity>]"},
				docoptOptions={
				"-s <srcLinkName>, --srcLinkName <srcLinkName>     Link name in source table",
				"-d <destLinkName>, --destLinkName <destLinkName>  Link name in dest table",
				"-m <multiplicity>, --multiplicity <multiplicity>  Multiplicity"},
		description="Create a new custom relational link",
		metaTags={CmdMeta.updatesDatabase},
		furtherHelp="If <srcLinkName> is not supplied then <destTableName> will be used if possible. "+
		"Similarly if <destLinkName> is not supplied then <srcTableName> will be used. Any configured link name "+
		"must be a valid DB identifier, e.g. my_link_1. "+
		"The <multiplicity> can be ONE_TO_ONE, ONE_TO_MANY, MANY_TO_ONE, the default is ONE_TO_ONE.") 
public class CreateLinkCommand extends ProjectSchemaModeCommand<CreateResult> {

	public static final String SRC_TABLE_NAME = "srcTableName";
	public static final String SRC_LINK_NAME = "srcLinkName";
	public static final String DEST_TABLE_NAME = "destTableName";
	public static final String DEST_LINK_NAME = "destLinkName";
	public static final String MULTIPLICITY = "multiplicity";

	private String srcTableName;
	private String srcLinkName;
	private String destTableName;
	private String destLinkName;
	private Link.Multiplicity multiplicity;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.srcTableName = PluginUtils.configureStringProperty(configElem, SRC_TABLE_NAME, true);
		this.destTableName = PluginUtils.configureStringProperty(configElem, DEST_TABLE_NAME, true);
		this.srcLinkName = PluginUtils.configureIdentifierProperty(configElem, SRC_LINK_NAME, false);
		this.destLinkName =PluginUtils.configureIdentifierProperty(configElem, DEST_LINK_NAME, false);
		this.multiplicity = Optional.ofNullable(PluginUtils.configureEnumProperty(Link.Multiplicity.class, configElem, MULTIPLICITY, false)).orElse(Multiplicity.ONE_TO_ONE);
		
		if(this.srcTableName.equals(destTableName)) {
			if(srcLinkName == null || destLinkName == null) {
				throw new CommandException(Code.COMMAND_USAGE_ERROR, "For reflexive links, both <srcLinkName> and <destLinkName> must be defined");
			}
		}
		if(srcLinkName != null && destLinkName != null && srcLinkName.equals(destLinkName)) {
			throw new CommandException(Code.COMMAND_USAGE_ERROR, "The <srcLinkName> and <destLinkName> must not be the same");
		}
		if(srcLinkName == null) {
			srcLinkName = destTableName;
		}
		if(destLinkName == null) {
			destLinkName = srcTableName;
		}
	}

	@Override
	public CreateResult execute(CommandContext cmdContext) {
		Project project = GlueDataObject.lookup(cmdContext, Project.class, Project.pkMap(getProjectName()));
		project.checkTableName(srcTableName);
		project.checkTableName(destTableName);
		
		Link link = GlueDataObject.create(cmdContext, Link.class, Link.pkMap(getProjectName(), this.srcTableName, this.srcLinkName), false);
		link.setMultiplicity(multiplicity.name());
		link.setDestTableName(destTableName);
		link.setDestLinkName(destLinkName);
		ModelBuilder.addLinkToModel(cmdContext.getGluetoolsEngine(), project, link);
		link.setProject(project);
		cmdContext.commit();
		return new CreateResult(Link.class, 1);
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
					return insideProjectMode.getProject().getTableNames()
							.stream()
							.map(n -> new CompletionSuggestion(n, true))
							.collect(Collectors.toList());
				}
			});
			registerVariableInstantiator("destTableName", new VariableInstantiator() {
				@Override
				public List<CompletionSuggestion> instantiate(
						ConsoleCommandContext cmdContext,
						@SuppressWarnings("rawtypes") Class<? extends Command> cmdClass, Map<String, Object> bindings,
						String prefix) {
					InsideProjectMode insideProjectMode = (ProjectSchemaMode) cmdContext.peekCommandMode();
					return insideProjectMode.getProject().getTableNames()
							.stream()
							.map(n -> new CompletionSuggestion(n, true))
							.collect(Collectors.toList());
				}
			});
			registerEnumLookup("multiplicity", Link.Multiplicity.class);
		}
	}


}

