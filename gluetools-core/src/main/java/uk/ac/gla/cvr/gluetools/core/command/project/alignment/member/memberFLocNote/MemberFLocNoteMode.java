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
package uk.ac.gla.cvr.gluetools.core.command.project.alignment.member.memberFLocNote;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandMode;
import uk.ac.gla.cvr.gluetools.core.command.configurableobject.ConfigurableObjectMode;
import uk.ac.gla.cvr.gluetools.core.command.project.alignment.member.MemberFLocNoteCommand;
import uk.ac.gla.cvr.gluetools.core.command.root.CommandModeClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ConfigurableTable;
import uk.ac.gla.cvr.gluetools.core.datamodel.memberFLocNote.MemberFLocNote;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;

@CommandModeClass(commandFactoryClass = MemberFLocNoteModeCommandFactory.class)
public class MemberFLocNoteMode extends CommandMode<MemberFLocNoteCommand> implements ConfigurableObjectMode {
	
	private String alignmentName;
	private String sourceName;
	private String sequenceID;
	private String refSeqName;
	private String featureName;
	private Project project;
	
	public MemberFLocNoteMode(Project project, MemberFLocNoteCommand command, 
			String alignmentName, 
			String sourceName, 
			String sequenceID, 
			String refSeqName,
			String featureName) {
		super(command, refSeqName, featureName);
		this.alignmentName = alignmentName;
		this.sourceName = sourceName;
		this.sequenceID = sequenceID;
		this.refSeqName = refSeqName;
		this.featureName = featureName;
		this.project = project;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void addModeConfigToCommandElem(Class<? extends Command> cmdClass,
			Element elem) {
		super.addModeConfigToCommandElem(cmdClass, elem);
		if(MemberFLocNoteModeCommand.class.isAssignableFrom(cmdClass)) {
			appendModeConfigToElem(elem, "refSeqName", refSeqName);
			appendModeConfigToElem(elem, "featureName", featureName);
		}
	}

	public String getAlignmentName() {
		return alignmentName;
	}
	
	public String getSourceName() {
		return sourceName;
	}

	public String getSequenceID() {
		return sequenceID;
	}

	public String getRefSeqName() {
		return refSeqName;
	}

	public String getFeatureName() {
		return featureName;
	}

	@Override
	public Project getProject() {
		return project;
	}

	@Override
	public String getTableName() {
		return ConfigurableTable.member_floc_note.name();
	}

	@Override
	public GlueDataObject getConfigurableObject(CommandContext cmdContext) {
		return lookupMemberFLocNote(cmdContext);
	}

	protected MemberFLocNote lookupMemberFLocNote(CommandContext cmdContext) {
		return GlueDataObject.lookup(cmdContext, MemberFLocNote.class, 
				MemberFLocNote.pkMap(getAlignmentName(), getSourceName(), getSequenceID(),
						getRefSeqName(), getFeatureName()));
	}


	
}
