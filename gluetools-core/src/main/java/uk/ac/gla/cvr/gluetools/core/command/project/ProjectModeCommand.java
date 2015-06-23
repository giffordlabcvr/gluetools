package uk.ac.gla.cvr.gluetools.core.command.project;

import java.util.ArrayList;
import java.util.List;

import org.apache.cayenne.ObjectContext;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandUsage;
import uk.ac.gla.cvr.gluetools.core.command.ListCommandResult;
import uk.ac.gla.cvr.gluetools.core.command.project.field.ListFieldsCommand;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._Sequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._Source;
import uk.ac.gla.cvr.gluetools.core.datamodel.field.Field;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

public abstract class ProjectModeCommand extends Command {

	private String projectName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		projectName = PluginUtils.configureStringProperty(configElem, "projectName", true);
	}

	protected String getProjectName() {
		return projectName;
	}

	protected Project getProject(ObjectContext objContext) {
		return GlueDataObject.lookup(objContext, Project.class, Project.pkMap(getProjectName()));
	}
	
	protected List<String> getValidSequenceFieldNames(CommandContext cmdContext) {
		Element listFieldsElem = CommandUsage.docElemForCmdClass(ListFieldsCommand.class);
		@SuppressWarnings("unchecked")
		ListCommandResult<Field> listFieldsResult = (ListCommandResult<Field>) cmdContext.executeElem(listFieldsElem);
		List<String> fieldNames = new ArrayList<String>();
		fieldNames.add(_Sequence.SEQUENCE_ID_PROPERTY);
		fieldNames.add(_Sequence.SOURCE_PROPERTY+"."+_Source.NAME_PROPERTY);
		fieldNames.add(_Sequence.FORMAT_PROPERTY);
		listFieldsResult.getResults().forEach(f -> fieldNames.add(f.getName()));
		return fieldNames;
	}

	
}
