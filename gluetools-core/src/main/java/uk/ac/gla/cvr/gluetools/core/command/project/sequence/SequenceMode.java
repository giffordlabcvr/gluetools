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
package uk.ac.gla.cvr.gluetools.core.command.project.sequence;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandMode;
import uk.ac.gla.cvr.gluetools.core.command.configurableobject.ConfigurableObjectMode;
import uk.ac.gla.cvr.gluetools.core.command.project.RenderableObjectMode;
import uk.ac.gla.cvr.gluetools.core.command.project.SequenceCommand;
import uk.ac.gla.cvr.gluetools.core.command.root.CommandModeClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ConfigurableTable;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;

@CommandModeClass(commandFactoryClass = SequenceModeCommandFactory.class)
public class SequenceMode extends CommandMode<SequenceCommand> implements ConfigurableObjectMode, RenderableObjectMode {

	private Project project;
	private String sourceName;
	private String sequenceID;
	
	public SequenceMode(Project project, SequenceCommand command, String sourceName, String sequenceID) {
		super(command, sourceName, sequenceID);
		this.project = project;
		this.sourceName = sourceName;
		this.sequenceID = sequenceID;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void addModeConfigToCommandElem(Class<? extends Command> cmdClass,
			Element elem) {
		super.addModeConfigToCommandElem(cmdClass, elem);
		if(SequenceModeCommand.class.isAssignableFrom(cmdClass)) {
			appendModeConfigToElem(elem, "sourceName", sourceName);
			appendModeConfigToElem(elem, "sequenceID", sequenceID);
		}
	}
	
	public Project getProject() {
		return project;
	}

	public String getSourceName() {
		return sourceName;
	}

	public String getSequenceID() {
		return sequenceID;
	}
	
	@Override
	public GlueDataObject getConfigurableObject(CommandContext cmdContext) {
		return lookupSequence(cmdContext);
	}

	public Sequence lookupSequence(CommandContext cmdContext) {
		return GlueDataObject.lookup(cmdContext, Sequence.class, Sequence.pkMap(getSourceName(), getSequenceID()));
	}

	@Override
	public String getTableName() {
		return ConfigurableTable.sequence.name();
	}

	@Override
	public GlueDataObject getRenderableObject(CommandContext cmdContext) {
		return lookupSequence(cmdContext);
	}

	
}
