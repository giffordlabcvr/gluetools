package uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta;

import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.biojava.nbio.core.sequence.DNASequence;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.sequenceSupplier.AbstractSequenceSupplier;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModulePluginCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ProvidedProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils.LineFeedStyle;

public abstract class BaseExportCommand<R extends CommandResult> extends ModulePluginCommand<R, FastaExporter> implements ProvidedProjectModeCommand {
	
	public static final String LINE_FEED_STYLE = "lineFeedStyle";

	private LineFeedStyle lineFeedStyle;


	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		lineFeedStyle = Optional.ofNullable(PluginUtils.configureEnumProperty(LineFeedStyle.class, configElem, LINE_FEED_STYLE, false)).orElse(LineFeedStyle.LF);
	}

	protected void export(CommandContext cmdContext, AbstractSequenceSupplier sequenceSupplier, 
			FastaExporter fastaExporter, PrintWriter printWriter) {
		AbstractSequenceConsumer sequenceConsumer = new AbstractSequenceConsumer() {
			@Override
			public void consumeSequence(CommandContext cmdContext, Sequence sequence) {
				String fastaId = fastaExporter.generateFastaId(sequence);
				printWriter.append(FastaUtils.seqIdCompoundsPairToFasta(fastaId,
						sequence.getSequenceObject().getNucleotides(cmdContext), lineFeedStyle));
				printWriter.flush();
			}
		};
		FastaExporter.doExport(cmdContext, sequenceSupplier, sequenceConsumer);
	}
	
	protected Map<String, DNASequence> export(CommandContext cmdContext, AbstractSequenceSupplier sequenceSupplier, 
			FastaExporter fastaExporter) {
		Map<String, DNASequence> ntFastaMap = new LinkedHashMap<String, DNASequence>();
		AbstractSequenceConsumer sequenceConsumer = new AbstractSequenceConsumer() {
			@Override
			public void consumeSequence(CommandContext cmdContext, Sequence sequence) {
				String fastaId = fastaExporter.generateFastaId(sequence);
				DNASequence ntSequence = FastaUtils.ntStringToSequence(sequence.getSequenceObject().getNucleotides(cmdContext));
				ntFastaMap.put(fastaId, ntSequence);
			}
		};
		FastaExporter.doExport(cmdContext, sequenceSupplier, sequenceConsumer);
		return ntFastaMap;
	}
	
}