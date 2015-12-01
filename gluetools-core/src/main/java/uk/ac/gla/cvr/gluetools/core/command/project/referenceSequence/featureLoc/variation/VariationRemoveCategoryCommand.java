package uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.featureLoc.variation;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.cayenne.BaseContext;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.CompletionSuggestion;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.DeleteResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.Variation;
import uk.ac.gla.cvr.gluetools.core.datamodel.vcatMembership.VcatMembership;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@CommandClass( 
		commandWords={"remove","category"}, 
		docoptUsages={"<vcatName>"},
		metaTags={CmdMeta.updatesDatabase},
		description="Remove variation from membership of a variation category", 
		furtherHelp="") 
public class VariationRemoveCategoryCommand extends VariationModeCommand<DeleteResult> {

	public static final String VCAT_NAME = "vcatName";

	private String vcatName;
	
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		vcatName = PluginUtils.configureStringProperty(configElem, VCAT_NAME, true);
		
	}

	@Override
	public DeleteResult execute(CommandContext cmdContext) {
		DeleteResult delResult = GlueDataObject.delete(cmdContext, VcatMembership.class, 
				VcatMembership.pkMap(getRefSeqName(), getFeatureName(), getVariationName(), vcatName), true);
		cmdContext.commit();
		((BaseContext) cmdContext.getObjectContext()).getQueryCache().removeGroup(VcatMembership.CACHE_GROUP);
		return delResult;
	
	}

	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerVariableInstantiator(VCAT_NAME, new AdvancedCmdCompleter.VariableInstantiator() {
				@SuppressWarnings("rawtypes")
				@Override
				protected List<CompletionSuggestion> instantiate(
						ConsoleCommandContext cmdContext,
						Class<? extends Command> cmdClass, Map<String, Object> bindings,
						String prefix) {
					VariationMode varMode = (VariationMode) cmdContext.peekCommandMode();
					Variation variation = GlueDataObject.lookup(cmdContext, Variation.class, 
							Variation.pkMap(varMode.getRefSeqName(), varMode.getFeatureName(), varMode.getVariationName()), true);
					if(variation != null) {
						return 
								variation.getVcatMemberships().stream()
								.map(vcm -> new CompletionSuggestion(vcm.getCategory().getName(), true))
								.collect(Collectors.toList());
					}
					return null;
				}
			});
		}
		
	}

}
