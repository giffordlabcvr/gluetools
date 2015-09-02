package uk.ac.gla.cvr.gluetools.core.command.project.feature;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.query.SelectQuery;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.ProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.feature.Feature;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.Variation;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


public abstract class FeatureModeCommand<R extends CommandResult> extends ProjectModeCommand<R> {


	private String featureName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		featureName = PluginUtils.configureStringProperty(configElem, "featureName", true);
	}

	protected String getFeatureName() {
		return featureName;
	}


	protected static FeatureMode getFeatureMode(CommandContext cmdContext) {
		return (FeatureMode) cmdContext.peekCommandMode();
	}


	protected Feature lookupFeature(CommandContext cmdContext) {
		return GlueDataObject.lookup(cmdContext.getObjectContext(), Feature.class, 
				Feature.pkMap(getFeatureName()));
	}

	@SuppressWarnings("rawtypes")
	public abstract static class VariationNameCompleter extends CommandCompleter {
		@Override
		public List<String> completionSuggestions(ConsoleCommandContext cmdContext, Class<? extends Command> cmdClass, List<String> argStrings) {
			if(argStrings.isEmpty()) {
				ObjectContext objContext = cmdContext.getObjectContext();
				List<Variation> variation = GlueDataObject.query(objContext, Variation.class, new SelectQuery(Variation.class));
				return variation.stream().map(Variation::getName).collect(Collectors.toList());
			}
			return super.completionSuggestions(cmdContext, cmdClass, argStrings);
		}
	}


}
