package uk.ac.gla.cvr.gluetools.core.digs;

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
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModuleProvidedCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ProvidedProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.OkResult;
import uk.ac.gla.cvr.gluetools.core.command.result.TableResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.sequenceGroup.SequenceGroup;
import uk.ac.gla.cvr.gluetools.programs.blast.dbManager.BlastDbManager;

@CommandClass(
		commandWords={"preview hits"}, 
		description = "Show a summary of hits for a given probe and target", 
		docoptUsages = { "-r <refName> -l <featureLoc> -g <targetGroup>" },
		docoptOptions = { 
				"-r <refName>, --probeReference <refName>       Reference sequence of the probe", 
				"-l <featureLoc>, --probeLocation <featureLoc>  Probe feature location",
				"-g <targetGroup>, --targetGroup <targetGroup>  Target group name"},
		metaTags = {}	
)
public class PreviewHitsCommand extends ModuleProvidedCommand<OkResult, DigsProber> implements ProvidedProjectModeCommand {

	private static final String REFERENCE_NAME = "refName";
	private static final String FEATURE_LOC = "featureLoc";
	private static final String TARGET_GROUP = "targetGroup";

	private String refName;
	private String featureLoc;
	private String targetGroup;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.refName = PluginUtils.configureStringProperty(configElem, REFERENCE_NAME, true);
		this.featureLoc = PluginUtils.configureStringProperty(configElem, FEATURE_LOC, true);
		this.targetGroup = PluginUtils.configureStringProperty(configElem, TARGET_GROUP, true);
	}

	@Override
	protected OkResult execute(CommandContext cmdContext, DigsProber digsProber) {
		BlastDbManager.getInstance().ensureSequenceGroupDB(cmdContext, targetGroup);
		return new OkResult();
	}

	public static class PreviewHitsResult extends TableResult {

		public PreviewHitsResult(String rootObjectName,
				List<String> columnHeaders, List<Map<String, Object>> rowData) {
			super(rootObjectName, columnHeaders, rowData);
			// TODO Auto-generated constructor stub
		}
		
	}
	
	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerDataObjectNameLookup("refName", ReferenceSequence.class, ReferenceSequence.NAME_PROPERTY);
			registerVariableInstantiator("featureLoc", new VariableInstantiator() {
				@Override
				protected List<CompletionSuggestion> instantiate(
						ConsoleCommandContext cmdContext,
						@SuppressWarnings("rawtypes") Class<? extends Command> cmdClass, Map<String, Object> bindings,
						String prefix) {
					ReferenceSequence refSequence = GlueDataObject.lookup(cmdContext, 
							ReferenceSequence.class, ReferenceSequence.pkMap((String) bindings.get("refName")), true);
					List<CompletionSuggestion> results = null;
					if(refSequence != null) {
						results = refSequence.getFeatureLocations().stream()
								.map(fLoc -> new CompletionSuggestion(fLoc.getFeature().getName(), true))
								.collect(Collectors.toList());
					}
					return results;
				}
			});
			registerDataObjectNameLookup("targetGroup", SequenceGroup.class, SequenceGroup.NAME_PROPERTY);
		}
		
	}
}