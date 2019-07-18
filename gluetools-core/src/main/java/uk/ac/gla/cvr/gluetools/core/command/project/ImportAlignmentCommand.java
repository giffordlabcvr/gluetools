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

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CommandException.Code;
import uk.ac.gla.cvr.gluetools.core.command.CommandFormatUtils;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.alignment.AlignmentAddMemberCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.alignment.member.MemberAddSegmentCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.CreateResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.document.CommandArray;
import uk.ac.gla.cvr.gluetools.core.document.CommandDocument;
import uk.ac.gla.cvr.gluetools.core.document.CommandObject;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass( 
	commandWords={"import","alignment"}, 
	docoptUsages={"-f <fileName>"},
	docoptOptions={
		"-f <fileName>, --fileName <fileName>  Alignment command document file"},
	description="Import an alignment from a command document file",
	furtherHelp="The format is a GLUE-specific (command document) JSON format, "+
	"produced by the 'export command-document' command in alignment mode.",
	metaTags={CmdMeta.consoleOnly}
	) 
public class ImportAlignmentCommand extends ProjectModeCommand<CreateResult> {

	public static final String FILENAME = "fileName";
	
	private String fileName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		fileName = PluginUtils.configureStringProperty(configElem, FILENAME, false);
	}

	@Override
	public CreateResult execute(CommandContext cmdContext) {
		byte[] jsonBytes = ((ConsoleCommandContext) cmdContext).loadBytes(fileName);
		CommandDocument almtCmdDocument = CommandFormatUtils.commandDocumentFromJsonString(new String(jsonBytes));
		String rootName = almtCmdDocument.getRootName();
		if(!rootName.equals("glueAlignment")) {
			throw new CommandException(Code.COMMAND_FAILED_ERROR, "Incorrect command document root name");
		}
		String alignmentName = almtCmdDocument.getString("name");
		if(alignmentName == null) {
			throw new CommandException(Code.COMMAND_FAILED_ERROR, "Missing alignment name");
		}
		ReferenceSequence refSequence = null;
		String refSeqName = almtCmdDocument.getString("refSequence");
		if(refSeqName != null) {
			refSequence = GlueDataObject.lookup(cmdContext, ReferenceSequence.class, ReferenceSequence.pkMap(refSeqName));
		}
		Alignment parentAlignment = null;
		String parentAlmtName = almtCmdDocument.getString("parentAlignment");
		if(parentAlmtName != null) {
			parentAlignment = GlueDataObject.lookup(cmdContext, Alignment.class, Alignment.pkMap(parentAlmtName));
		}
		Alignment newAlignment = CreateAlignmentCommand.createAlignment(cmdContext, alignmentName, refSequence, parentAlignment);
		CommandArray membersArray = almtCmdDocument.getArray("members");
		if(membersArray != null) {
			for(int i = 0; i < membersArray.size(); i++) {
				CommandObject memberObj = membersArray.getObject(i);
				String sourceName = memberObj.getString("sourceName");
				if(sourceName == null) {
					throw new CommandException(Code.COMMAND_FAILED_ERROR, "Missing member sourceName");
				}
				String sequenceID = memberObj.getString("sequenceID");
				if(sequenceID == null) {
					throw new CommandException(Code.COMMAND_FAILED_ERROR, "Missing member sequenceID");
				}
				Boolean referenceMember = memberObj.getBoolean("referenceMember");
				if(referenceMember == null) {
					throw new CommandException(Code.COMMAND_FAILED_ERROR, "Missing member referenceMember field");
				}
				Sequence sequence = GlueDataObject.lookup(cmdContext, Sequence.class, Sequence.pkMap(sourceName, sequenceID));
				AlignmentMember newAlmtMember = AlignmentAddMemberCommand.addMember(cmdContext, newAlignment, sequence, referenceMember);
				CommandArray segmentsArray = memberObj.getArray("segments");
				if(segmentsArray != null) {
					for(int j = 0; j < segmentsArray.size(); j++) {
						CommandObject segmentObj = segmentsArray.getObject(j);
						Integer refStart = segmentObj.getInteger("refStart");
						if(refStart == null) {
							throw new CommandException(Code.COMMAND_FAILED_ERROR, "Missing segment refStart field");
						}
						Integer refEnd = segmentObj.getInteger("refEnd");
						if(refEnd == null) {
							throw new CommandException(Code.COMMAND_FAILED_ERROR, "Missing segment refEnd field");
						}
						Integer memberStart = segmentObj.getInteger("memberStart");
						if(memberStart == null) {
							throw new CommandException(Code.COMMAND_FAILED_ERROR, "Missing segment memberStart field");
						}
						Integer memberEnd = segmentObj.getInteger("memberEnd");
						if(memberEnd == null) {
							throw new CommandException(Code.COMMAND_FAILED_ERROR, "Missing segment memberEnd field");
						}
						MemberAddSegmentCommand.addSegment(cmdContext, newAlmtMember, refStart, refEnd, memberStart, memberEnd);
					}
				}
			}
		}
		cmdContext.commit();
		return new CreateResult(Alignment.class, 1);
	}

	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerPathLookup("fileName", false);
		}


	}
}
