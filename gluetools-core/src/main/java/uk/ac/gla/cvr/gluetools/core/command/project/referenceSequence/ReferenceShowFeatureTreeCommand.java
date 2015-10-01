package uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence;

import java.util.List;

import org.apache.cayenne.query.SelectQuery;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandUtils;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.feature.Feature;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceFeatureTreeResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@CommandClass(
		commandWords={"show", "feature", "tree"},
		docoptUsages={"[-r] [<featureName>]"},
		docoptOptions={"-r, --realized  Include NT and AA segments"},
		description="Show a tree of features for which the reference has a location",
		furtherHelp="If <featureName> is supplied, the result tree will only contain locations of features which "+
		"are ancestors or descendents of that specific feature. Otherwise, all feature locations will be present. Feature "+
		"locations at the same tree level are listed in order of refStart."
)
public class ReferenceShowFeatureTreeCommand extends ReferenceSequenceModeCommand<ReferenceFeatureTreeResult> {

	public static final String REALIZED = "realized";
	private static final String FEATURE_NAME = "featureName";
	
	private String featureName;
	private Boolean realized;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.featureName = PluginUtils.configureStringProperty(configElem, FEATURE_NAME, false);
		this.realized = PluginUtils.configureBooleanProperty(configElem, REALIZED, true);
	}

	@Override
	public ReferenceFeatureTreeResult execute(CommandContext cmdContext) {
		ReferenceSequence refSeq = lookupRefSeq(cmdContext);
		Feature limitingFeature = null;
		if(featureName != null) {
			limitingFeature = GlueDataObject.lookup(cmdContext.getObjectContext(), Feature.class, Feature.pkMap(featureName));
		}
		boolean recursive = true;
		if(realized) {
			return refSeq.getRealisedFeatureTree(cmdContext, limitingFeature, recursive);
		} else {
			return refSeq.getFeatureTree(cmdContext, limitingFeature, recursive);
		}
	}

	@CompleterClass
	public static class Completer extends CommandCompleter {

		@SuppressWarnings("rawtypes")
		@Override
		public List<String> completionSuggestions(ConsoleCommandContext cmdContext, Class<? extends Command> cmdClass, List<String> argStrings) {
			if(argStrings.isEmpty() || argStrings.size() == 1 && argStrings.get(0).matches("-r|--realized")) {
				return CommandUtils.runListCommand(cmdContext, Feature.class, new SelectQuery(Feature.class)).
						getColumnValues(Feature.NAME_PROPERTY);
			}
			return super.completionSuggestions(cmdContext, cmdClass, argStrings);
		}
		
	}

}
