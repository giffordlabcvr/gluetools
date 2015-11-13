package uk.ac.gla.cvr.gluetools.core.command.project;

import java.util.Arrays;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.project.ProjectModeCommandException.Code;
import uk.ac.gla.cvr.gluetools.core.command.result.UpdateResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.field.Field;
import uk.ac.gla.cvr.gluetools.core.datamodel.field.FieldType;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@CommandClass( 
		commandWords={"copy","sequence-field"}, 
		docoptUsages={"(-w <whereClause> | -a) <fromFieldName> <toFieldName> [-b <batchSize>]"},
		metaTags={CmdMeta.updatesDatabase},
		docoptOptions={
				"-w <whereClause>, --whereClause <whereClause>  Qualify updated sequences", 
				"-a, --allSequences                             Update all sequences",
				"-b <batchSize>, --batchSize <batchSize>        Update batch size" },
		description="Copy values between fields for multiple sequences", 
		furtherHelp="Updates to the database are committed in batches, the default batch size is 250.") 
public class CopySequenceFieldCommand extends MultiSequenceFieldUpdateCommand {

	public static final String FROM_FIELD_NAME = "fromFieldName";
	public static final String TO_FIELD_NAME = "toFieldName";
	
	private String fromFieldName;
	private String toFieldName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		fromFieldName = PluginUtils.configureStringProperty(configElem, FROM_FIELD_NAME, true);
		toFieldName = PluginUtils.configureStringProperty(configElem, TO_FIELD_NAME, true);
	}

	@Override
	public UpdateResult execute(CommandContext cmdContext) {
		Project project = getProjectMode(cmdContext).getProject();
		project.checkValidCustomSequenceFieldNames(Arrays.asList(fromFieldName, toFieldName));
		Field fromField = project.getSequenceField(fromFieldName);
		Field toField = project.getSequenceField(toFieldName);
		FieldType fromFieldType = fromField.getFieldType();
		FieldType toFieldType = toField.getFieldType();
		if(!fromFieldType.equals(toFieldType)) {
			throw new ProjectModeCommandException(Code.INCOMPATIBLE_TYPES_FOR_COPY, fromFieldName, fromFieldType.name(), toFieldName, toFieldType.name());
		}
		return executeUpdates(cmdContext);
	}
	
	@Override
	protected void updateSequence(CommandContext cmdContext, Sequence sequence) {
		Object fromValue = sequence.readProperty(fromFieldName);
		sequence.writeProperty(toFieldName, fromValue);
	}

	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerVariableInstantiator("fromFieldName", new SequenceFieldInstantiator());
			registerVariableInstantiator("toFieldName", new SequenceFieldInstantiator());
		}
	}
	
}
