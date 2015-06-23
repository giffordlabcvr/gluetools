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
import uk.ac.gla.cvr.gluetools.core.command.project.sequence.ListSequencesCommand;
import uk.ac.gla.cvr.gluetools.core.datamodel.DataModelException;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.DataModelException.Code;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._Sequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._Source;
import uk.ac.gla.cvr.gluetools.core.datamodel.field.Field;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.XmlUtils;

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
		List<String> fieldNames = getCustomSequenceFieldNames(cmdContext);
		fieldNames.add(_Sequence.SEQUENCE_ID_PROPERTY);
		fieldNames.add(_Sequence.SOURCE_PROPERTY+"."+_Source.NAME_PROPERTY);
		fieldNames.add(_Sequence.FORMAT_PROPERTY);
		return fieldNames;
	}

	protected List<String> getCustomSequenceFieldNames(CommandContext cmdContext) {
		Element listFieldsElem = CommandUsage.docElemForCmdClass(ListFieldsCommand.class);
		@SuppressWarnings("unchecked")
		ListCommandResult<Field> listFieldsResult = (ListCommandResult<Field>) cmdContext.executeElem(listFieldsElem);
		List<String> fieldNames = new ArrayList<String>();
		listFieldsResult.getResults().forEach(f -> fieldNames.add(f.getName()));
		return fieldNames;
	}

	
	protected Sequence lookupSequence(CommandContext cmdContext, 
			String sourceName, String sequenceID, boolean allowNull) {
		Element listSequencesElem = CommandUsage.docElemForCmdClass(ListSequencesCommand.class);
		XmlUtils.appendElementWithText(listSequencesElem, "sourceName", sourceName);
		@SuppressWarnings("unchecked")
		ListCommandResult<Sequence> listResult = (ListCommandResult<Sequence>) cmdContext.executeElem(listSequencesElem);
		List<Sequence> results = listResult.getResults();
		if(results.size() == 0) {
			if(allowNull) {
				return null;
			} else {
				throw new DataModelException(Code.OBJECT_NOT_FOUND, Sequence.class.getSimpleName(), 
						Sequence.pkMap(sourceName, sequenceID));
			}
		} else if(results.size() > 1) {
			throw new DataModelException(Code.MULTIPLE_OBJECTS_FOUND, Sequence.class.getSimpleName(), 
					Sequence.pkMap(sourceName, sequenceID));
		} else {
			return results.get(0);
		}
	}
	
}
