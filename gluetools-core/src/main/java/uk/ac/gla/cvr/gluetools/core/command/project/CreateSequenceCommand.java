package uk.ac.gla.cvr.gluetools.core.command.project;

import java.util.Base64;

import org.apache.cayenne.ObjectContext;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.result.CreateResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.SequenceException;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.SequenceException.Code;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.SequenceFormat;
import uk.ac.gla.cvr.gluetools.core.datamodel.source.Source;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass( 
	commandWords={"create", "sequence"}, 
	docoptUsages={
		"<sourceName> <sequenceID> <format> -f <file>",
		"<sourceName> <sequenceID> <format> --base64 <data>"
	}, 
	docoptOptions={
		"-f <file>, --fileName <file>  File containing the sequence data", 
		"--base64 <data>  Sequence data encoded as Base64"},
	description="Create a new sequence") 
public class CreateSequenceCommand extends ProjectModeCommand {

	public static final String SOURCE_NAME = "sourceName";
	public static final String SEQUENCE_ID = "sequenceID";
	public static final String FORMAT = "format";
	public static final String FILE_NAME = "fileName";
	public static final String BASE64 = "base64";

	private String sourceName;
	private String sequenceID;
	private SequenceFormat format;
	private String fileName;
	private byte[] dataFromBase64;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		sourceName = PluginUtils.configureStringProperty(configElem, SOURCE_NAME, true);
		sequenceID = PluginUtils.configureStringProperty(configElem, SEQUENCE_ID, true);
		format = PluginUtils.configureEnumProperty(SequenceFormat.class, configElem, FORMAT, true);
		fileName = PluginUtils.configureStringProperty(configElem, FILE_NAME, false);
		String base64String = PluginUtils.configureStringProperty(configElem, BASE64, false);
		if(fileName == null && base64String == null) {
			throw new SequenceException(Code.NO_DATA_PROVIDED);
		}
		if(base64String != null) {
			try {
				dataFromBase64 = Base64.getDecoder().decode(base64String);
			} catch (IllegalArgumentException e) {
				throw new SequenceException(e, Code.BASE_64_FORMAT_EXCEPTION, e.getMessage());
			}
		}
	}

	@Override
	public CommandResult execute(CommandContext cmdContext) {
		ObjectContext objContext = cmdContext.getObjectContext();
		Sequence sequence = GlueDataObject.create(objContext, Sequence.class, Sequence.pkMap(sourceName, sequenceID));
		Source source = GlueDataObject.lookup(objContext, Source.class, Source.pkMap(sourceName));
		sequence.setSource(source);
		sequence.setFormat(format.name());
		byte[] sequenceData = null;
		if(fileName != null) {
			sequenceData = ((ConsoleCommandContext) cmdContext).loadBytes(fileName);
		} else if(dataFromBase64 != null){
			sequenceData = dataFromBase64;
		}
		format.nucleotidesAsString(sequenceData); // check for format errors here.
		sequence.setOriginalData(sequenceData);
		return new CreateResult(Sequence.class, 1);
	}

}
