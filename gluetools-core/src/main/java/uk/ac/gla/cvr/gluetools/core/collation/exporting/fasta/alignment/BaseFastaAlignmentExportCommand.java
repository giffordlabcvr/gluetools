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
package uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment;

import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.Map;

import uk.ac.gla.cvr.gluetools.utils.fasta.DNASequence;import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.memberSupplier.QueryMemberSupplier;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModulePluginCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ProvidedProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;

public abstract class BaseFastaAlignmentExportCommand<R extends CommandResult> extends ModulePluginCommand<R, FastaAlignmentExporter> implements ProvidedProjectModeCommand {

	private FastaAlignmentExportCommandDelegate delegate = new FastaAlignmentExportCommandDelegate();
	
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		delegate.configure(pluginConfigContext, configElem, false);
	}

	protected void exportAlignment(CommandContext cmdContext, PrintWriter printWriter, FastaAlignmentExporter fastaAlmtExporter) {
		QueryMemberSupplier memberSupplier = new QueryMemberSupplier(delegate.getAlignmentName(), delegate.getRecursive(), delegate.getWhereClause());
		AbstractStringAlmtRowConsumer almtRowConsumer = new AbstractStringAlmtRowConsumer() {
			@Override
			public void consumeAlmtRow(CommandContext cmdContext, AlignmentMember almtMember, String alignmentRowString) {
				String fastaId = FastaAlignmentExporter.generateFastaId(fastaAlmtExporter.getIdTemplate(), almtMember);
				String fastaAlmtRowString = FastaUtils.seqIdCompoundsPairToFasta(fastaId, alignmentRowString, delegate.getLineFeedStyle());
				printWriter.append(fastaAlmtRowString);
				printWriter.flush();
			}
		};
		FastaAlignmentExporter.exportAlignment(cmdContext, 
				delegate.getNucleotideAlignmentColumnsSelector(cmdContext), 
				delegate.getExcludeEmptyRows(), memberSupplier, almtRowConsumer);
	}
	
	protected Map<String, DNASequence> exportAlignment(CommandContext cmdContext, FastaAlignmentExporter fastaAlmtExporter) {
		Map<String, DNASequence> ntFastaMap = new LinkedHashMap<String, DNASequence>();
		QueryMemberSupplier memberSupplier = new QueryMemberSupplier(delegate.getAlignmentName(), delegate.getRecursive(), delegate.getWhereClause());
		AbstractStringAlmtRowConsumer almtRowConsumer = new AbstractStringAlmtRowConsumer() {
			@Override
			public void consumeAlmtRow(CommandContext cmdContext, AlignmentMember almtMember, String alignmentRowString) {
				String fastaId = FastaAlignmentExporter.generateFastaId(fastaAlmtExporter.getIdTemplate(), almtMember);
				ntFastaMap.put(fastaId, FastaUtils.ntStringToSequence(alignmentRowString));
			}
		};
		FastaAlignmentExporter.exportAlignment(cmdContext, 
				delegate.getNucleotideAlignmentColumnsSelector(cmdContext), 
				delegate.getExcludeEmptyRows(), memberSupplier, almtRowConsumer);
		return ntFastaMap;
	}

}