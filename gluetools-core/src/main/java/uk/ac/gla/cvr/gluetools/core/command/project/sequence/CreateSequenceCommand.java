package uk.ac.gla.cvr.gluetools.core.command.project.sequence;

import org.apache.cayenne.ObjectContext;
import org.w3c.dom.Element;

import com.sun.org.apache.xml.internal.security.exceptions.Base64DecodingException;
import com.sun.org.apache.xml.internal.security.utils.Base64;

import uk.ac.gla.cvr.gluetools.core.collation.sequence.CollatedSequenceFormat;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.CreateCommandResult;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.ProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.Project;
import uk.ac.gla.cvr.gluetools.core.datamodel.Sequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.Source;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigException;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigException.Code;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@PluginClass(elemName="create-sequence")
@CommandClass(description="Create a new sequence", 
	docoptUsages={"<sourceName> <sequenceID> <format> (-f <file> | -b64 <data>)"}, 
	docoptOptions={"-f <file>, --file <file>  File containing the sequence data", 
		"-b64 <data>, --base64 <data>  Sequence data encoded as Base64"}) 
public class CreateSequenceCommand extends ProjectModeCommand {

	private String sourceName;
	private String sequenceID;
	private CollatedSequenceFormat format;
	private String file;
	private byte[] dataFromBase64;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		sourceName = PluginUtils.configureString(configElem, "sourceName/text()", true);
		sequenceID = PluginUtils.configureString(configElem, "sequenceID/text()", true);
		format = PluginUtils.configureEnum(CollatedSequenceFormat.class, configElem, "format/text()", true);
		file = PluginUtils.configureString(configElem, "file/text()", false);
		String base64String = PluginUtils.configureString(configElem, "base64/text()", false);
		if(file == null && base64String == null) {
			throw new PluginConfigException(Code.CONFIG_CONSTRAINT_VIOLATION, "Data not provided");
		}
		if(file != null && base64String != null) {
			throw new PluginConfigException(Code.CONFIG_CONSTRAINT_VIOLATION, "Ambiguous data source");
		}
		if(base64String != null) {
			try {
				dataFromBase64 = Base64.decode(base64String);
			} catch (Base64DecodingException e) {
				throw new PluginConfigException(e, Code.CONFIG_FORMAT_ERROR, "base64/text", e.getMessage(), base64String);
			}
		}
	}

	@Override
	public CommandResult execute(CommandContext cmdContext) {
		ObjectContext objContext = cmdContext.getGluetoolsEngine().getCayenneObjectContext();
		Sequence sequence = GlueDataObject.create(objContext, Sequence.class, Sequence.pkMap(sourceName, sequenceID));
		Project project = getProject(objContext);
		Source source = GlueDataObject.lookup(objContext, Source.class, Source.pkMap(project.getName(), sourceName));
		sequence.setSource(source);
		sequence.setFormat(format.name());
		if(file != null) {
			sequence.setData(((ConsoleCommandContext) cmdContext).loadBytes(file));
		} else if(dataFromBase64 != null){
			sequence.setData(dataFromBase64);
		}
		objContext.commitChanges();
		return new CreateCommandResult(sequence.getObjectId());
	}

}
