package uk.ac.gla.cvr.gluetools.core.command.project.variationCategory;

import java.util.List;
import java.util.Map;

import org.apache.cayenne.exp.ExpressionFactory;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.CompletionSuggestion;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.OkResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.variationCategory.VariationCategory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass( 
		commandWords={"set", "parent"},
		docoptUsages={"<parentName>"},
		metaTags={CmdMeta.updatesDatabase},
		description="Specify the parent for this variation category",
		furtherHelp="Loops arising from parent relationships are not allowed."
	) 
public class VariationCategorySetParentCommand extends VariationCategoryModeCommand<OkResult> {

	public static final String PARENT_VCAT_NAME = "parentName";
	private String parentVcatName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		parentVcatName = PluginUtils.configureStringProperty(configElem, PARENT_VCAT_NAME, true);
	}

	@Override
	public OkResult execute(CommandContext cmdContext) {
		VariationCategory feature = lookupVariationCategory(cmdContext);
		VariationCategory parentFeature = GlueDataObject.lookup(cmdContext, VariationCategory.class, VariationCategory.pkMap(parentVcatName));
		feature.setParent(parentFeature);
		cmdContext.commit();
		return new OkResult();
	}

	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {

		public Completer() {
			super();
			registerVariableInstantiator("parentName", new VariableInstantiator() {
				@Override
				@SuppressWarnings("rawtypes")
				protected List<CompletionSuggestion> instantiate(
						ConsoleCommandContext cmdContext, Class<? extends Command> cmdClass,
						Map<String, Object> bindings, String prefix) {
					String thisVCatName = ((VariationCategoryMode) cmdContext.peekCommandMode()).getVcatName();
					return listNames(cmdContext, prefix, VariationCategory.class, VariationCategory.NAME_PROPERTY, 
							ExpressionFactory.noMatchExp(VariationCategory.NAME_PROPERTY, thisVCatName));
				}
			});

		}
		
		
	}
	
}
