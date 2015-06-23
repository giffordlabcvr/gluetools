package uk.ac.gla.cvr.gluetools.core.command.root;

import org.apache.cayenne.ObjectContext;

import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;

public abstract class RootModeCommand extends Command {

	protected Project getProject(ObjectContext objContext, String projectName) {
		return GlueDataObject.lookup(objContext, Project.class, Project.pkMap(projectName));
	}
	
	
}
