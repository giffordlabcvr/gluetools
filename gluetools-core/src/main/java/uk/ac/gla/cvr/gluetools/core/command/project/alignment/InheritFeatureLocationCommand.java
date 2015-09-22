package uk.ac.gla.cvr.gluetools.core.command.project.alignment;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.alignment.InheritFeatureLocationCommand.InheritFeatureLocationResult;
import uk.ac.gla.cvr.gluetools.core.command.project.alignment.InheritFeatureLocationException.Code;
import uk.ac.gla.cvr.gluetools.core.command.result.TableResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.feature.Feature;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceFeatureTreeResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@CommandClass(
		commandWords={"inherit", "feature-location"}, 
		docoptUsages={"[-r] <featureName>]"},
		docoptOptions={"-r, --recursive  Add locations for the feature's descendents"},
		description="Inherit a feature location from parent alignment", 
		furtherHelp="This command adds feature locations to the reference sequence of this alignment, based on the feature locations "+
		"of the parent alignment's reference sequence. A location for the named feature and its ancestors will be added. The locations are derived from the reference sequence's alignment to the parent "+
		"reference. If the recursive option is used, this means that a location will not only be inherited for the named feature, but "+
		"also for any child features of the named feature, their children, etc.") 
public class InheritFeatureLocationCommand extends AlignmentModeCommand<InheritFeatureLocationResult>{

	public static final String FEATURE_NAME = "featureName";
	public static final String RECURSIVE = "recursive";

	private boolean recursive;
	private String featureName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.recursive = PluginUtils.configureBooleanProperty(configElem, RECURSIVE, true);
		this.featureName = PluginUtils.configureStringProperty(configElem, FEATURE_NAME, true);
	}


	@Override
	public InheritFeatureLocationResult execute(CommandContext cmdContext) {
		Alignment thisAlignment = lookupAlignment(cmdContext);
		ReferenceSequence thisRefSeq = thisAlignment.getRefSequence();
		Alignment parentAlignment = thisAlignment.getParent();
		if(parentAlignment == null) {
			throw new InheritFeatureLocationException(Code.NO_PARENT_ALIGNMENT, thisAlignment.getName());
		}
		Feature feature = GlueDataObject.lookup(cmdContext.getObjectContext(), Feature.class, Feature.pkMap(featureName));
		ReferenceSequence parentRefSeq = parentAlignment.getRefSequence();
		ReferenceFeatureTreeResult parentRefFeatureTree = parentRefSeq.getFeatureTree(cmdContext, feature, recursive);
		
		
		return null;
	}
	
	
	public static class InheritFeatureLocationResult extends TableResult {

		public InheritFeatureLocationResult(List<String> columnHeaders, List<Map<String, Object>> rowData) {
			super("inheritFeatureLocationResult", columnHeaders, rowData);
		}
		
	}

	@CompleterClass
	public static class Completer extends FeatureLocNameCompleter {

		@SuppressWarnings("rawtypes")
		@Override
		public List<String> completionSuggestions(
				ConsoleCommandContext cmdContext,
				Class<? extends Command> cmdClass, List<String> argStrings) {
			if(argStrings.isEmpty() || argStrings.get(0).equals("-r") || argStrings.get(0).equals("--recursive")) {
				return super.completionSuggestions(cmdContext, cmdClass, argStrings);
			}
			return Collections.emptyList();
		}
		
		
		
	}
	
}
