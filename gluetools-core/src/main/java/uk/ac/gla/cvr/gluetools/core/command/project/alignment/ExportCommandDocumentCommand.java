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
package uk.ac.gla.cvr.gluetools.core.command.project.alignment;

import java.io.StringWriter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.json.Json;
import javax.json.stream.JsonGenerator;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.OkResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignedSegment.AlignedSegment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.document.CommandArray;
import uk.ac.gla.cvr.gluetools.core.document.CommandDocument;
import uk.ac.gla.cvr.gluetools.core.document.CommandObject;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.CommandDocumentJsonUtils;



@CommandClass( 
	commandWords={"export","command-document"}, 
	docoptUsages={"-f <fileName>"},
	docoptOptions={
		"-f <fileName>, --fileName <fileName>  Alignment command document file"},
	description="Export an alignment to a command document file",
	furtherHelp="The format is a GLUE-specific (command document) JSON format, "+
	"consumed by the 'import alignment' command in project mode.",
	metaTags={CmdMeta.consoleOnly}
	) 
public class ExportCommandDocumentCommand extends AlignmentModeCommand<OkResult> {

	public static final String FILENAME = "fileName";
	
	private String fileName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		fileName = PluginUtils.configureStringProperty(configElem, FILENAME, false);
	}

	@Override
	public OkResult execute(CommandContext cmdContext) {
		Alignment alignment = lookupAlignment(cmdContext);
		CommandDocument almtCmdDocument = new CommandDocument("glueAlignment");
		almtCmdDocument.setString("name", alignment.getName());
		ReferenceSequence refSequence = alignment.getRefSequence();
		if(refSequence != null) {
			almtCmdDocument.setString("refSequence", refSequence.getName());
			Alignment parentAlmt = alignment.getParent();
			if(parentAlmt != null) {
				almtCmdDocument.setString("parentAlignment", parentAlmt.getName());
			}
		}
		CommandArray membersArray = almtCmdDocument.setArray("members");
		alignment.getMembers().forEach(member -> {
			CommandObject memberObj = membersArray.addObject();
			memberObj.setString("sourceName", member.getSequence().getSource().getName());
			memberObj.setString("sequenceID", member.getSequence().getSequenceID());
			memberObj.setBoolean("referenceMember", member.getReferenceMember());
			List<AlignedSegment> alignedSegments = member.getAlignedSegments();
			if(alignedSegments.size() > 0) {
				CommandArray segmentsArray = memberObj.setArray("segments");
				alignedSegments.forEach(seg -> {
					CommandObject segmentObj = segmentsArray.addObject();
					segmentObj.setInt("refStart", seg.getRefStart());
					segmentObj.setInt("refEnd", seg.getRefEnd());
					segmentObj.setInt("memberStart", seg.getMemberStart());
					segmentObj.setInt("memberEnd", seg.getMemberEnd());
				});
			}				
		});
		StringWriter stringWriter = new StringWriter();
		Map<String, Boolean> config = new LinkedHashMap<String, Boolean>();
		config.put(JsonGenerator.PRETTY_PRINTING, true);
		JsonGenerator jsonGenerator = Json.createGeneratorFactory(config).createGenerator(stringWriter);
		CommandDocumentJsonUtils.commandDocumentGenerateJson(jsonGenerator, almtCmdDocument);
		jsonGenerator.flush();
		byte[] jsonBytes = stringWriter.toString().getBytes();		
		((ConsoleCommandContext) cmdContext).saveBytes(fileName, jsonBytes);
		return new OkResult();
	}

	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerPathLookup("fileName", false);
		}


	}
}
