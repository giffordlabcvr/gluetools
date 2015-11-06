package uk.ac.gla.cvr.gluetools.core.command.project;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.field.Field;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@CommandClass( 
		commandWords={"set","sequence","field"}, 
		docoptUsages={"(-w <whereClause> | -a) <fieldName> <fieldValue> [-b <batchSize>]"},
		metaTags={CmdMeta.updatesDatabase},
		docoptOptions={
				"-w <whereClause>, --whereClause <whereClause>  Qualify updated sequences", 
				"-a, --allSequences                             Update all sequences",
				"-b <batchSize>, --batchSize <batchSize>        Update batch size" },
		description="Set a field's value for one or more sequences", 
		furtherHelp="Updates to the database are committed in batches, the default batch size is 250.") 
public class SetSequenceFieldCommand extends MultiSequenceFieldUpdateCommand {

	public static final String FIELD_VALUE = "fieldValue";
	
	private String fieldValue;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		fieldValue = PluginUtils.configureStringProperty(configElem, FIELD_VALUE, true);
	}


	@Override
	protected void updateSequence(CommandContext cmdContext, Sequence sequence, String fieldName) {
		Project project = getProjectMode(cmdContext).getProject();
		Field field = project.getSequenceField(fieldName);
		Object newValue = field.getFieldType().getFieldTranslator().valueFromString(fieldValue);
		sequence.writeProperty(fieldName, newValue);
	}

	@CompleterClass
	public static class Completer extends SequenceFieldNameCompleter {}
	
}
