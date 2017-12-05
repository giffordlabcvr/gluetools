package uk.ac.gla.cvr.gluetools.core.collation.referenceBuilder;

import java.util.Map;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandMode;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModulePluginCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ProvidedProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.CreateResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.source.Source;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@CommandClass(
		commandWords={"build", "reference"}, 
		description = "Build a reference sequence from Genbank metadata", 
		docoptUsages = { "<sourceName> <sequenceID> -r <refName>" },
		docoptOptions = { "-r <refName>, --refName <refName>  Name of the reference sequence to create" },
		metaTags = {CmdMeta.updatesDatabase},
		furtherHelp = "The named sequence must exist and be in Genbank XML format."
)
public class BuildReferenceCommand extends ModulePluginCommand<CreateResult, GbRefBuilder> implements ProvidedProjectModeCommand {

	private static final String SOURCE_NAME = "sourceName";
	private static final String SEQUENCE_ID = "sequenceID";
	private static final String REF_NAME = "refName";

	private String sourceName;
	private String sequenceID;
	private String refName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		sourceName = PluginUtils.configureStringProperty(configElem, SOURCE_NAME, true);
		sequenceID = PluginUtils.configureStringProperty(configElem, SEQUENCE_ID, true);
		refName = PluginUtils.configureStringProperty(configElem, REF_NAME, true);
	}

	@Override
	protected CreateResult execute(CommandContext cmdContext, GbRefBuilder genbankReferenceBuilder) {
		return genbankReferenceBuilder.buildReference(cmdContext, refName, sourceName, sequenceID);
	}

	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerDataObjectNameLookup("sourceName", Source.class, Source.NAME_PROPERTY);
			registerVariableInstantiator("sequenceID", 
					new QualifiedDataObjectNameInstantiator(Sequence.class, Sequence.SEQUENCE_ID_PROPERTY) {
				@Override
				@SuppressWarnings("rawtypes")
				protected void qualifyResults(CommandMode cmdMode,
						Map<String, Object> bindings, Map<String, Object> qualifierValues) {
					qualifierValues.put(Sequence.SOURCE_NAME_PATH, bindings.get("sourceName"));
				}
			});
		}
	}
	
}
