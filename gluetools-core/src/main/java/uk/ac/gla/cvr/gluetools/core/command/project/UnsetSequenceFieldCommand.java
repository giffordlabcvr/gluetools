package uk.ac.gla.cvr.gluetools.core.command.project;

import java.util.Collections;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.result.UpdateResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@CommandClass( 
		commandWords={"unset", "sequence-field"}, 
		docoptUsages={"(-w <whereClause> | -a) <fieldName>  [-b <batchSize>]"},
		metaTags={CmdMeta.updatesDatabase},
		docoptOptions={
				"-w <whereClause>, --whereClause <whereClause>  Qualify updated sequences", 
				"-a, --allSequences                             Update all sequences",
				"-b <batchSize>, --batchSize <batchSize>        Update batch size" },
		description="Unset a field's value for one or more sequences", 
		furtherHelp="Unsetting means reverting the field value to null. Updates to the database are committed in batches, the default batch size is 250.") 
public class UnsetSequenceFieldCommand extends MultiSequenceFieldUpdateCommand {

	public static final String FIELD_NAME = "fieldName";

	private String fieldName;

	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		fieldName = PluginUtils.configureStringProperty(configElem, FIELD_NAME, true);
	}
	
	@Override
	public UpdateResult execute(CommandContext cmdContext) {
		Project project = getProjectMode(cmdContext).getProject();
		project.checkValidCustomSequenceFieldNames(Collections.singletonList(fieldName));

		return executeUpdates(cmdContext);
	}

	@Override
	protected void updateSequence(CommandContext cmdContext, Sequence sequence) {
		sequence.writeProperty(fieldName, null);
	}

	@CompleterClass
	public static class Completer extends SequenceFieldNameCompleter {}

}
