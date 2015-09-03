package uk.ac.gla.cvr.gluetools.core.command.project;

import org.apache.cayenne.ObjectContext;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.OkResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.AbstractSequenceObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass( 
	commandWords={"export", "sequence"}, 
	docoptUsages={
		"<sourceName> <sequenceID> <fileName>"
	}, 
	metaTags = { CmdMeta.consoleOnly },
	furtherHelp=
		"The <fileName> names the file which will the sequence data",
	description="Export the original sequence data to a file") 
public class ExportSequenceCommand extends ProjectModeCommand<OkResult> {

	public static final String SOURCE_NAME = "sourceName";
	public static final String SEQUENCE_ID = "sequenceID";
	public static final String FILE_NAME = "fileName";

	private String sourceName;
	private String sequenceID;
	private String fileName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		sourceName = PluginUtils.configureStringProperty(configElem, SOURCE_NAME, true);
		sequenceID = PluginUtils.configureStringProperty(configElem, SEQUENCE_ID, true);
		fileName = PluginUtils.configureStringProperty(configElem, FILE_NAME, true);
	}

	@Override
	public OkResult execute(CommandContext cmdContext) {
		ObjectContext objContext = cmdContext.getObjectContext();
		Sequence sequence = GlueDataObject.lookup(objContext, Sequence.class, Sequence.pkMap(sourceName, sequenceID), false);
		AbstractSequenceObject sequenceObject = sequence.getSequenceObject();
		byte[] sequenceBytes = sequenceObject.toOriginalData();
		((ConsoleCommandContext) cmdContext).saveBytes(fileName, sequenceBytes);
		return new OkResult();
	}

}
