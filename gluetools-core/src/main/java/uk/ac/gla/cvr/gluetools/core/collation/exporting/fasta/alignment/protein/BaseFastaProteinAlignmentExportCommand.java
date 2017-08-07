package uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment.protein;

import java.io.PrintWriter;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment.AbstractAlmtRowConsumer;
import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment.FastaAlignmentExportCommandDelegate;
import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.memberSupplier.QueryMemberSupplier;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModulePluginCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ProvidedProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;

public abstract class BaseFastaProteinAlignmentExportCommand<R extends CommandResult> extends ModulePluginCommand<R, FastaProteinAlignmentExporter> implements ProvidedProjectModeCommand {

	private FastaAlignmentExportCommandDelegate delegate = new FastaAlignmentExportCommandDelegate();
	
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		delegate.configure(pluginConfigContext, configElem, true);
	}

	protected FastaAlignmentExportCommandDelegate getDelegate() {
		return delegate;
	}

	protected void exportProteinAlignment(CommandContext cmdContext, FastaProteinAlignmentExporter almtExporter, PrintWriter printWriter) {
		QueryMemberSupplier queryMemberSupplier = new QueryMemberSupplier(delegate.getAlignmentName(), delegate.getRecursive(), delegate.getWhereClause());

		AbstractAlmtRowConsumer almtRowConsumer = new AbstractAlmtRowConsumer() {
			@Override
			public void consumeAlmtRow(CommandContext cmdContext, AlignmentMember almtMember, String alignmentRowString) {
				String fastaId = FastaProteinAlignmentExporter.generateFastaId(almtExporter.getIdTemplate(), almtMember);
				printWriter.append(FastaUtils.seqIdCompoundsPairToFasta(fastaId, alignmentRowString, delegate.getLineFeedStyle()));
				printWriter.flush();
			}
		};
		FastaProteinAlignmentExporter.exportAlignment(cmdContext,
					delegate.getFeatureName(), delegate.getAlignmentColumnsSelector(cmdContext), delegate.getExcludeEmptyRows(), 
					queryMemberSupplier, 
					almtRowConsumer);
	}
	
}