package uk.ac.gla.cvr.gluetools.core.command.root.projectschema;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
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
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@CommandClass( 
		commandWords={"delete", "link"}, 
		docoptUsages={"<srcTableName> <srcLinkName>"},
		description="Delete a custom relational link",
		metaTags={CmdMeta.updatesDatabase}) 
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
		Project project = GlueDataObject.lookup(cmdContext, Project.class, Project.pkMap(getProjectName()));
		Link link = GlueDataObject.lookup(cmdContext, Link.class, Link.pkMap(getProjectName(), srcTableName, srcLinkName), false);
		ModelBuilder.deleteLinkFromModel(cmdContext.getGluetoolsEngine(), project, link);
		GlueDataObject.delete(cmdContext, Link.class, Link.pkMap(getProjectName(), srcTableName, srcLinkName), false);
		cmdContext.commit();
		return new DeleteResult(Link.class, 1);
	}

	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerVariableInstantiator("srcTableName", new VariableInstantiator() {
				@Override
				protected List<CompletionSuggestion> instantiate(
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
				protected List<CompletionSuggestion> instantiate(
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

