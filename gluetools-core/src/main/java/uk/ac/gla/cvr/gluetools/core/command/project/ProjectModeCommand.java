package uk.ac.gla.cvr.gluetools.core.command.project;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.cayenne.query.SelectQuery;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandUsage;
import uk.ac.gla.cvr.gluetools.core.command.CommandUtils;
import uk.ac.gla.cvr.gluetools.core.command.ListCommandResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.DataModelException;
import uk.ac.gla.cvr.gluetools.core.datamodel.DataModelException.Code;
import uk.ac.gla.cvr.gluetools.core.datamodel.module.Module;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.utils.XmlUtils;

public abstract class ProjectModeCommand extends Command {

	
	protected ProjectMode getProjectMode(CommandContext cmdContext) {
		ProjectMode projectMode = (ProjectMode) cmdContext.peekCommandMode();
		return projectMode;
	}

	
	protected Sequence lookupSequence(CommandContext cmdContext, 
			String sourceName, String sequenceID, boolean allowNull) {
		Element listSequencesElem = CommandUsage.docElemForCmdClass(ListSequencesCommand.class);
		XmlUtils.appendElementWithText(listSequencesElem, "sourceName", sourceName);
		XmlUtils.appendElementWithText(listSequencesElem, "sequenceID", sequenceID);
		@SuppressWarnings("unchecked")
		ListCommandResult<Sequence> listResult = (ListCommandResult<Sequence>) cmdContext.
			executeElem(listSequencesElem.getOwnerDocument().getDocumentElement());
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
	
	public abstract static class ModuleNameCompleter extends CommandCompleter {
		@Override
		public List<String> completionSuggestions(CommandContext cmdContext, List<String> argStrings) {
			LinkedList<String> suggestions = new LinkedList<String>();
			if(argStrings.isEmpty()) {
				suggestions.addAll(CommandUtils.runListCommand(cmdContext, Module.class, new SelectQuery(Module.class)).
						getResults().stream().map(Module::getName).collect(Collectors.toList()));
			}
			return suggestions;
		}
	}

	
}
