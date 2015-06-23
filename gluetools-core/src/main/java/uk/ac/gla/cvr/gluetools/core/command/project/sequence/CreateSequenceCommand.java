package uk.ac.gla.cvr.gluetools.core.command.project.sequence;

import java.util.Base64;

import org.apache.cayenne.ObjectContext;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.CreateCommandResult;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.ProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.SequenceException;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.SequenceException.Code;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.SequenceFormat;
import uk.ac.gla.cvr.gluetools.core.datamodel.source.Source;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@PluginClass(elemName="create-sequence")
@CommandClass(description="Create a new sequence", 
	docoptUsages={
		"<sourceName> <sequenceID> <format> -f <file>",
		"<sourceName> <sequenceID> <format> --base64 <data>"
	}, 
	docoptOptions={"-f <file>, --file <file>  File containing the sequence data", 
		"--base64 <data>  Sequence data encoded as Base64"}) 
public class CreateSequenceCommand extends ProjectModeCommand {

	private String sourceName;
	private String sequenceID;
	private SequenceFormat format;
	private String file;
	private byte[] dataFromBase64;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		sourceName = PluginUtils.configureStringProperty(configElem, "sourceName", true);
		sequenceID = PluginUtils.configureStringProperty(configElem, "sequenceID", true);
		format = PluginUtils.configureEnumProperty(SequenceFormat.class, configElem, "format", true);
		file = PluginUtils.configureStringProperty(configElem, "file", false);
		String base64String = PluginUtils.configureStringProperty(configElem, "base64", false);
		if(file == null && base64String == null) {
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
		if(file != null) {
			sequence.setData(((ConsoleCommandContext) cmdContext).loadBytes(file));
		} else if(dataFromBase64 != null){
			sequence.setData(dataFromBase64);
		}
		try {
			sequence.getSequenceDoc(); // checks XML format
		} catch(Exception e) {
			throw new SequenceException(e, Code.CREATE_FROM_FILE_FAILED, file);
		}
		return new CreateCommandResult(sequence.getObjectId());
	}

}
