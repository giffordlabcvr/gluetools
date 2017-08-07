package uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment;

import java.io.PrintWriter;
import java.util.Map;

import org.w3c.dom.Element;

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
		AbstractAlmtRowConsumer almtRowConsumer = new AbstractAlmtRowConsumer() {
			@Override
			public void consumeAlmtRow(CommandContext cmdContext, Map<String, String> memberPkMap, AlignmentMember almtMember,
					String alignmentRowString) {
				String fastaId = FastaAlignmentExporter.generateFastaId(fastaAlmtExporter.getIdTemplate(), almtMember);
				String fastaAlmtRowString = FastaUtils.seqIdCompoundsPairToFasta(fastaId, alignmentRowString, delegate.getLineFeedStyle());
				printWriter.append(fastaAlmtRowString);
				printWriter.flush();
			}
		};
		FastaAlignmentExporter.exportAlignment(cmdContext, 
				delegate.getAlignmentColumnsSelector(cmdContext), 
				delegate.getExcludeEmptyRows(), memberSupplier, almtRowConsumer);
	}
	
	
}