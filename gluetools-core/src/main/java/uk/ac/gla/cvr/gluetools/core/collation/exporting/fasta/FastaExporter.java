package uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta;

import java.util.List;

import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.sequenceSupplier.AbstractSequenceSupplier;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;

@PluginClass(elemName="fastaExporter")
public class FastaExporter extends AbstractFastaExporter<FastaExporter> {


	public FastaExporter() {
		super();
		addModulePluginCmdClass(ExportCommand.class);
		addModulePluginCmdClass(WebExportCommand.class);
		addModulePluginCmdClass(ExportMemberCommand.class);
		addModulePluginCmdClass(WebExportMemberCommand.class);
	}

	public void doExport(CommandContext cmdContext, 
			AbstractSequenceSupplier sequenceSupplier, 
			AbstractSequenceConsumer sequenceConsumer) {
		
		int batchSize = 500;
		int processed = 0;
		int offset = 0;
		int totalNumSeqs = sequenceSupplier.countSequences(cmdContext);

		while(processed < totalNumSeqs) {
			List<Sequence> sequences = sequenceSupplier.supplySequences(cmdContext, offset, batchSize);
			sequences.forEach(seq -> {
				sequenceConsumer.consumeSequence(cmdContext, seq);
			});
			offset += batchSize;
			processed += sequences.size();
			GlueLogger.getGlueLogger().finest("Processed "+processed+" of "+totalNumSeqs+" sequences");
			cmdContext.newObjectContext();
		}
	}
	
}
