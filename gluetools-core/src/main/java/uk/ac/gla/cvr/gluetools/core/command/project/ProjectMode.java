/**
 *    GLUE: A flexible system for virus sequence data
 *    Copyright (C) 2018 The University of Glasgow
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Affero General Public License as published
 *    by the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Affero General Public License for more details.

 *    You should have received a copy of the GNU Affero General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *    Contact details:
 *    MRC-University of Glasgow Centre for Virus Research
 *    Sir Michael Stoker Building, Garscube Campus, 464 Bearsden Road, 
 *    Glasgow G61 1QH, United Kingdom
 *    
 *    Josh Singer: josh.singer@glasgow.ac.uk
 *    Rob Gifford: robert.gifford@glasgow.ac.uk
*/
package uk.ac.gla.cvr.gluetools.core.command.project;

import org.apache.cayenne.configuration.server.ServerRuntime;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandMode;
import uk.ac.gla.cvr.gluetools.core.command.DbContextChangingMode;
import uk.ac.gla.cvr.gluetools.core.command.root.CommandModeClass;
import uk.ac.gla.cvr.gluetools.core.command.root.ProjectCommand;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ModelBuilder;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;

@CommandModeClass(commandFactoryClass = ProjectModeCommandFactory.class)
public class ProjectMode extends CommandMode<ProjectCommand> implements InsideProjectMode, DbContextChangingMode {

	
	private Project project;
	private ServerRuntime newServerRuntime;
	
	public ProjectMode(CommandContext cmdContext, ProjectCommand command, Project project) {
		super(command, project.getName());
		this.project = project;
		setNewServerRuntime(ModelBuilder.createProjectModel(cmdContext.getGluetoolsEngine(), project));
	}
	
	public Project getProject() {
		return project;
	}

	public ServerRuntime getNewServerRuntime() {
		return newServerRuntime;
	}

	public void setNewServerRuntime(ServerRuntime newServerRuntime) {
		this.newServerRuntime = newServerRuntime;
	}
	
	@Override
	public ServerRuntime getServerRuntime() {
		return newServerRuntime;
	}

	@Override
	public void exit() {
		newServerRuntime.shutdown();
	}
	
}
