package uk.ac.gla.cvr.gluetools.core.command.project;

import org.apache.cayenne.ObjectContext;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.CreateResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.SequenceFormat;
import uk.ac.gla.cvr.gluetools.core.datamodel.source.Source;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass( 
	commandWords={"create", "sequence"}, 
	docoptUsages={
		"<sourceName> <sequenceID> <format> <originalData>"
	}, 
	metaTags={CmdMeta.updatesDatabase},
	docoptOptions={
		"--base64 <data>  Sequence data encoded as Base64"},
	description="Create a new sequence") 
public class CreateSequenceCommand extends ProjectModeCommand<CreateResult> {

	public static final String SOURCE_NAME = "sourceName";
	public static final String SEQUENCE_ID = "sequenceID";
	public static final String FORMAT = "format";
	public static final String ORIGINAL_DATA = "originalData";

	private String sourceName;
	private String sequenceID;
	private SequenceFormat format;
	private byte[] originalData;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		sourceName = PluginUtils.configureStringProperty(configElem, SOURCE_NAME, true);
		sequenceID = PluginUtils.configureStringProperty(configElem, SEQUENCE_ID, true);
		format = PluginUtils.configureEnumProperty(SequenceFormat.class, configElem, FORMAT, true);
		originalData = PluginUtils.configureBase64BytesProperty(configElem, ORIGINAL_DATA, true);
	}

	@Override
	public CreateResult execute(CommandContext cmdContext) {
		ObjectContext objContext = cmdContext.getObjectContext();
		Sequence sequence = GlueDataObject.create(objContext, Sequence.class, Sequence.pkMap(sourceName, sequenceID), false);
		Source source = GlueDataObject.lookup(objContext, Source.class, Source.pkMap(sourceName));
		sequence.setSource(source);
		sequence.setFormat(format.name());
		format.nucleotidesAsString(originalData); // check for format errors here.
		sequence.setOriginalData(originalData);
		cmdContext.commit();
		return new CreateResult(Sequence.class, 1);
	}

}
