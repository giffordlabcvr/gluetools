package uk.ac.gla.cvr.gluetools.core.command.project;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.CreateResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.SequenceFormat;
import uk.ac.gla.cvr.gluetools.core.datamodel.source.Source;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass( 
	commandWords={"import", "sequence"}, 
	docoptUsages={
		"<sourceName> <sequenceID> <format> <fileName>"
	}, 
	metaTags = { CmdMeta.consoleOnly, CmdMeta.updatesDatabase },
	furtherHelp=
		"The <fileName> names the file containing the sequence data",
	description="Import a new sequence from a file") 
public class ImportSequenceCommand extends ProjectModeCommand<CreateResult> {

	public static final String SOURCE_NAME = "sourceName";
	public static final String SEQUENCE_ID = "sequenceID";
	public static final String FORMAT = "format";
	public static final String FILE_NAME = "fileName";

	private String sourceName;
	private String sequenceID;
	private SequenceFormat format;
	private String fileName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		sourceName = PluginUtils.configureStringProperty(configElem, SOURCE_NAME, true);
		sequenceID = PluginUtils.configureStringProperty(configElem, SEQUENCE_ID, true);
		format = PluginUtils.configureEnumProperty(SequenceFormat.class, configElem, FORMAT, true);
		fileName = PluginUtils.configureStringProperty(configElem, FILE_NAME, true);
	}

	@Override
	public CreateResult execute(CommandContext cmdContext) {
		
		Sequence sequence = CreateSequenceCommand.createSequence(cmdContext, sourceName, sequenceID, false);
		Source source = GlueDataObject.lookup(cmdContext, Source.class, Source.pkMap(sourceName));
		sequence.setSource(source);
		sequence.setFormat(format.name());
		byte[] sequenceData = ((ConsoleCommandContext) cmdContext).loadBytes(fileName);
		sequence.setOriginalData(sequenceData);
		cmdContext.commit();
		return new CreateResult(Sequence.class, 1);
	}

	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerDataObjectNameLookup("sourceName", Source.class, Source.NAME_PROPERTY);
			registerEnumLookup("format", SequenceFormat.class);
			registerPathLookup("fileName", false);
		}
	}

}
