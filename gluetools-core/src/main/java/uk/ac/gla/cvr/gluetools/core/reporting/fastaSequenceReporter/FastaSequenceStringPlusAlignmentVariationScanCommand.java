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
package uk.ac.gla.cvr.gluetools.core.reporting.fastaSequenceReporter;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ProvidedProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.document.CommandDocument;
import uk.ac.gla.cvr.gluetools.core.document.CommandObject;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;
import uk.ac.gla.cvr.gluetools.utils.fasta.DNASequence;

@CommandClass(
		commandWords={"string-plus-alignment", "variation", "scan"}, 
		description = "Scan a FASTA string for variations, given a precomputed alignment with the target reference", 
		docoptUsages = {},
		docoptOptions = {},
		metaTags = { CmdMeta.inputIsComplex }	
)
public class FastaSequenceStringPlusAlignmentVariationScanCommand extends FastaSequenceBaseVariationScanCommand 
	implements ProvidedProjectModeCommand{

	public static final String FASTA_STRING = "fastaString";
	public static final String QUERY_TO_TARGET_SEGS = "queryToTargetSegs";
	
	private String fastaString;
	private List<QueryAlignedSegment> queryToTargetSegs = new ArrayList<QueryAlignedSegment>();
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.fastaString = PluginUtils.configureStringProperty(configElem, FASTA_STRING, true);
		CommandDocument qaSegsCmdDoc = PluginUtils.configureCommandDocumentProperty(configElem, QUERY_TO_TARGET_SEGS, true);
		qaSegsCmdDoc.getArray("alignedSegment").getItems().forEach(item -> {
			queryToTargetSegs.add(new QueryAlignedSegment((CommandObject) item));
		});
	}

	@Override
	protected CommandResult execute(CommandContext cmdContext, FastaSequenceReporter fastaSequenceReporter) {
		DNASequence fastaNTSeq = FastaUtils.ntStringToSequence(fastaString);
		return executeAux(cmdContext, fastaSequenceReporter, "querySequence", fastaNTSeq, getTargetRefName(), queryToTargetSegs);
	}

}
