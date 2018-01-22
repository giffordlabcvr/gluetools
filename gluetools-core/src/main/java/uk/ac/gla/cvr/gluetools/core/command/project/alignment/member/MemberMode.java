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
package uk.ac.gla.cvr.gluetools.core.command.project.alignment.member;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandMode;
import uk.ac.gla.cvr.gluetools.core.command.configurableobject.ConfigurableObjectMode;
import uk.ac.gla.cvr.gluetools.core.command.project.InsideProjectMode;
import uk.ac.gla.cvr.gluetools.core.command.project.RenderableObjectMode;
import uk.ac.gla.cvr.gluetools.core.command.project.alignment.InsideAlignmentMode;
import uk.ac.gla.cvr.gluetools.core.command.project.alignment.MemberCommand;
import uk.ac.gla.cvr.gluetools.core.command.root.CommandModeClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ConfigurableTable;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;

@CommandModeClass(commandFactoryClass = MemberModeCommandFactory.class)
public class MemberMode extends CommandMode<MemberCommand> implements InsideProjectMode, InsideAlignmentMode, RenderableObjectMode, ConfigurableObjectMode {

	
	private Project project;
	private String almtName;
	private String sourceName;
	private String sequenceID;
	
	public MemberMode(Project project, MemberCommand command, String almtName, String sourceName, String sequenceID) {
		super(command, sourceName, sequenceID);
		this.project = project;
		this.almtName = almtName;
		this.sourceName = sourceName;
		this.sequenceID = sequenceID;
	}
	
	@SuppressWarnings("rawtypes")
	@Override
	public void addModeConfigToCommandElem(Class<? extends Command> cmdClass,
			Element elem) {
		super.addModeConfigToCommandElem(cmdClass, elem);
		if(MemberModeCommand.class.isAssignableFrom(cmdClass)) {
			appendModeConfigToElem(elem, "sourceName", sourceName);
			appendModeConfigToElem(elem, "sequenceID", sequenceID);
		}
	}
	
	

	public Project getProject() {
		return project;
	}

	public String getAlignmentName() {
		return almtName;
	}
	
	public AlignmentMember getAlignmentMember(CommandContext cmdContext) {
		return GlueDataObject.lookup(cmdContext, AlignmentMember.class, AlignmentMember.pkMap(getAlignmentName(), sourceName, sequenceID));
	}

	@Override
	public GlueDataObject getRenderableObject(CommandContext cmdContext) {
		return getAlignmentMember(cmdContext);
	}

	@Override
	public String getTableName() {
		return ConfigurableTable.alignment_member.name();
	}

	@Override
	public GlueDataObject getConfigurableObject(CommandContext cmdContext) {
		return getAlignmentMember(cmdContext);
	}
	
	
}
